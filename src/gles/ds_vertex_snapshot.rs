//! Experimental owned client-array packet. Caller retains the native fallback.
//! No Environment, raw pointers, GL operations or unsafe Send/Sync.
#[derive(Clone,Copy)]
pub enum Source<'a> {
    Client(&'a [u8]),
    Buffer { id:u32, offset:usize, bytes:usize },
}
pub struct Array<'a> { pub components:usize,pub component_bytes:usize,pub stride:usize,pub source:Source<'a> }
#[derive(Debug)]
pub enum OwnedSource { Client(Vec<u8>),Buffer { id:u32,offset:usize } }
#[derive(Debug)]
pub struct OwnedArray { pub stride:usize,pub width:usize,pub source:OwnedSource }
#[derive(Debug)]
pub struct Packet { pub indices:Vec<u16>,pub arrays:Vec<OwnedArray>,pub first:u16,pub bytes:usize }
#[derive(Debug,PartialEq)]
pub enum Fallback { Empty,Layout,Overflow,Bounds,Budget }
pub fn capture(indices:&[u16],arrays:&[Array<'_>],budget:usize)->Result<Packet,Fallback> {
    let first=*indices.iter().min().ok_or(Fallback::Empty)?;
    let last=*indices.iter().max().unwrap();
    if arrays.is_empty()||arrays.len()>16{return Err(Fallback::Layout);}
    let mut bytes=indices.len().checked_mul(2).ok_or(Fallback::Overflow)?;
    let mut plan=Vec::with_capacity(arrays.len());
    for array in arrays {
        if !(1..=4).contains(&array.components)||![1,2,4].contains(&array.component_bytes){return Err(Fallback::Layout);}
        let width=array.components*array.component_bytes;
        let stride=if array.stride==0 {width}else{array.stride};
        let start=(first as usize).checked_mul(stride).ok_or(Fallback::Overflow)?;
        let len=(last as usize-first as usize).checked_mul(stride).and_then(|v|v.checked_add(width)).ok_or(Fallback::Overflow)?;
        let end=start.checked_add(len).ok_or(Fallback::Overflow)?;
        match array.source {
            Source::Client(source)=>{
                if end>source.len(){return Err(Fallback::Bounds);}
                bytes=bytes.checked_add(len).ok_or(Fallback::Overflow)?;
            },
            Source::Buffer {offset,bytes,..}=>{
                if offset.checked_add(end).ok_or(Fallback::Overflow)?>bytes{return Err(Fallback::Bounds);}
            },
        }
        if bytes>budget{return Err(Fallback::Budget);}
        plan.push((stride,width,start,len));
    }
    // Validate the complete packet before copying. Duplicate/order/topology are
    // retained; all attributes, including VBO offsets, use the same rebase.
    let mut owned=Vec::with_capacity(arrays.len());
    for (array,(stride,width,start,len))in arrays.iter().zip(plan) {
        let source=match array.source {
            Source::Client(source)=>OwnedSource::Client(source[start..start+len].to_vec()),
            Source::Buffer {id,offset,..}=>OwnedSource::Buffer {id,offset:offset+start},
        };
        owned.push(OwnedArray {stride,width,source});
    }
    Ok(Packet {indices:indices.iter().map(|index|index-first).collect(),arrays:owned,first,bytes})
}
#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn mixed_client_vbo_rebase_preserves_every_indexed_attribute_byte() {
        for trial in 0..96usize {
            let width=1+trial%4;let stride=trial%19;
            let step=if stride==0{width*4}else{stride};
            let source:Vec<u8>=(0..4096).map(|i|((i*37+trial*11)%251)as u8).collect();
            let indices:Vec<u16>=(0..81).map(|i|(17+(i*13+trial)%67)as u16).collect();
            let arrays=[Array {components:width,component_bytes:4,stride,source:Source::Client(&source)},Array {components:width,component_bytes:4,stride,source:Source::Buffer {id:7,offset:29,bytes:8192}}];
            let packet=capture(&indices,&arrays,65536).unwrap();
            let OwnedSource::Client(ref bytes)=packet.arrays[0].source else{panic!()};
            let OwnedSource::Buffer {id,offset}=packet.arrays[1].source else{panic!()};
            assert_eq!(id,7);assert_eq!(indices.len(),packet.indices.len());
            for (old,new)in indices.iter().zip(&packet.indices) {
                let a=*old as usize*step;let b=*new as usize*step;
                assert_eq!(&source[a..a+width*4],&bytes[b..b+width*4]);
                assert_eq!(29+a,offset+b);
            }
        }
    }
    #[test]
    fn queued_packet_survives_source_mutation_and_reallocation() {
        let mut source:Vec<u8>=(0..=255).collect();
        let packet=capture(&[7,9,7,8],&[Array {components:4,component_bytes:1,stride:0,source:Source::Client(&source)}],1024).unwrap();
        let expected=source[28..40].to_vec();let(tx,rx)=std::sync::mpsc::channel();
        let worker=std::thread::spawn(move||{rx.recv().unwrap();let OwnedSource::Client(bytes)=packet.arrays.into_iter().next().unwrap().source else{panic!()};bytes});
        source.fill(0);source.resize(8192,99);tx.send(()).unwrap();
        assert_eq!(worker.join().unwrap(),expected);
    }
    #[test]
    fn unsupported_or_unbounded_packets_fall_back_before_any_submission() {
        let source=[0u8;32];
        let a=[Array {components:4,component_bytes:4,stride:0,source:Source::Client(&source)}];
        assert!(matches!(capture(&[0,4],&a,1024),Err(Fallback::Bounds)));
        assert!(matches!(capture(&[0,1],&a,8),Err(Fallback::Budget)));
        assert!(matches!(capture(&[],&a,1024),Err(Fallback::Empty)));
        let a=[Array {components:3,component_bytes:4,stride:usize::MAX,source:Source::Buffer {id:1,offset:0,bytes:usize::MAX}}];
        assert!(matches!(capture(&[2,3],&a,usize::MAX),Err(Fallback::Overflow)));
        let a=[Array {components:3,component_bytes:4,stride:0,source:Source::Buffer {id:1,offset:usize::MAX,bytes:usize::MAX}}];
        assert!(matches!(capture(&[0,1],&a,usize::MAX),Err(Fallback::Overflow)));
    }
}
