from pathlib import Path
import argparse,json,re,sys
r=Path(__file__).resolve().parents[2]
p=argparse.ArgumentParser();p.add_argument('--check',action='store_true');options=p.parse_args()
native=(r/'src/gles/gles1_native.rs').read_text(encoding='utf-8');start=native.index("impl GLES for GLES1Native<'_> {");end=native.index('\n}\n',start);part=native[start:end]
signatures=list(re.finditer(r'^    (unsafe )?fn (\w+)\s*\((.*?)\)\s*(?:->\s*([^\{]+))?\{',part,re.M|re.S))
assert len(signatures)>180,len(signatures)
generated=['// Generated from GLES1Native signatures; pointer calls synchronously finish before return.','use super::gles11_raw::types::*;','use super::gles_generic::GLchar;','impl GLES for Proxy {'];names=[]
for m in signatures:
 unsafe,name,args,result=m.groups();names.append(name);result=(result or '()').strip()
 parsed=[]
 for arg in args.split(','):
  arg=arg.strip()
  if not arg or arg in ['&self','&mut self']:continue
  a,ty=arg.split(':',1);a=a.strip().removeprefix('mut ');ty=ty.strip();assert re.fullmatch(r'[A-Za-z_][A-Za-z_0-9]*',a),(name,a)
  assert not ty.startswith('&'),(name,ty)
  parsed.append((a,ty))
 signature='    '+('unsafe 'if unsafe else'')+'fn '+name+'('+args+')'+(' -> '+result if result!='()'else'')+' {';generated.append(signature)
 for arg,ty in parsed:
  if ty.startswith('*'):generated.append('        let '+arg+' = '+arg+' as usize;')
 call_args=', '.join(arg+' as '+ty if ty.startswith('*')else arg for arg,ty in parsed)
 call=('unsafe { g.'+name+'('+call_args+') }')if unsafe else('g.'+name+'('+call_args+')')
 pointer_result=result.startswith('*');call=('('+call+') as usize')if pointer_result else call
 operation='move |driver: &mut NativeDriverState| { let mut g = driver.borrow_native(); '+call+' }'
 scalar_async=result=='()'and not any(ty.startswith('*')for _,ty in parsed)and name not in ['Finish','Flush','DrawArrays','DrawArraysInstanced','DrawElements','DrawElementsInstanced']
 if scalar_async:generated+=['        if self.actor.queued { self.actor.submit(self.context, '+operation+'); }','        else { self.actor.call(self.context, '+operation+'); }']
 else:generated.append('        self.actor.call(self.context, '+operation+')'+(' as '+result if pointer_result else''))
 generated.append('    }')
generated.append('}')
proxy='\n'.join(generated)+'\n'
assert len(names)==len(set(names))

target=r/'src/gles/ds_render_proxy.rs'
if options.check:
 if target.read_text(encoding='utf-8')!=proxy:raise SystemExit('Native GLES proxy differs; run generator')
else:target.write_text(proxy,encoding='utf-8')
print(json.dumps({'proxy_methods':len(names),'pointer_calls_synchronous':True,'async_draws':False,'checked':options.check}))
