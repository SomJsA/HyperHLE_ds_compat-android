//! Replay a fully fingerprinted ARM first-PUSH and run the unchanged body.
use crate::{abi::GuestFunction, cpu::Cpu, mem::MutPtr, Environment};
pub(crate) fn original_after_push(env: &mut Environment, entry: u32, push: u32) {
    let mask = push & 0xffff;
    assert!(
        push & 0xffff_0000 == 0xe92d_0000
            && mask & (1 << 14) != 0
            && mask & ((1 << 13) | (1 << 15)) == 0,
        "Unsupported original ARM prologue"
    );
    let regs = *env.cpu.regs();
    let bytes = mask.count_ones() * 4;
    let sp = regs[Cpu::SP]
        .checked_sub(bytes)
        .expect("Original native stack underflow");
    let ret = env.dyld.return_to_host_routine().addr_with_thumb_bit();
    let mut offset = 0;
    for register in 0..15 {
        if mask & (1 << register) == 0 {
            continue;
        }
        let value = if register == Cpu::LR {
            ret
        } else {
            regs[register]
        };
        env.mem.write(MutPtr::<u32>::from_bits(sp + offset), value);
        offset += 4;
    }
    env.cpu.regs_mut()[Cpu::SP] = sp;
    GuestFunction::from_addr_and_thumb_flag(entry + 4, false).call_without_pushing_stack_frame(env);
    assert_eq!(
        env.cpu.regs()[Cpu::SP],
        regs[Cpu::SP],
        "Original native stack contract changed"
    );
    env.cpu
        .branch(GuestFunction::from_addr_with_thumb_bit(regs[Cpu::LR]));
}
