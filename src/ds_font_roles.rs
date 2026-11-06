//! Android Dead Space faces and verified native drawing owners.
//! Scene routing is independent of PC input, Java overlay readiness and HUD probes.
use crate::font::Font;
use crate::{abi::GuestFunction, cpu::Cpu, mem::{ConstPtr, ConstVoidPtr, MutPtr}, Environment};
use std::collections::HashMap;

pub(crate) const TITLE: &[u8] = include_bytes!("../res/overlay/TitleChinese.ttf");
pub(crate) const STORY: &[u8] = include_bytes!("../res/overlay/StoryChinese.ttf");

#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash)]
pub(crate) enum Role { Frontend, Story }

const GET_ID: u32 = 0x0054_5fdc;
const CG_GET_ID: u32 = 0x0038_b474;
const ID_COUNTER: u32 = 0x00ae_ed94;
const MAX_KEYS: usize = 512;
const MENU_STATE: u32 = 0x001b_8648;
const MENU_DRAW: u32 = 0x001b_d604;
const WORLD_DRAW: u32 = 0x0008_1968;

#[derive(Default)]
pub(crate) struct State {
    checked: bool,
    armed: bool,
    keys: HashMap<(u32, Role), u32>,
    calls: u64,
    class_calls: [u64; 2],
    role_calls: u64,
    face_calls: [u64; 2],
    fallback: u64,
    trace: bool,
    scene_story: bool,
    scene_calls: [u64; 3],
    scene_changes: u32,
}
impl State {
    fn observe_scene(&mut self, next: Option<Role>) {
        let Some(next) = next else { return; };
        let story = next == Role::Story;
        if self.scene_story != story {
            self.scene_story = story;
            self.scene_changes += 1;
            if self.trace && self.scene_changes <= 64 {
                echo!("[DSfont-scene] role={next:?} changes={}", self.scene_changes);
            }
        }
    }
    fn key(&mut self, original: u32, role: Role, counter: &mut u32) -> Option<u32> {
        if let Some(key) = self.keys.get(&(original, role)) { return Some(*key); }
        if original == 0 || original > *counter || self.keys.len() >= MAX_KEYS { return None; }
        let next = counter.checked_add(1)?;
        *counter = next;
        self.keys.insert((original, role), next);
        Some(next)
    }
}
impl Drop for State {
    fn drop(&mut self) {
        if self.armed {
            echo!("[DSfont-key] end calls={} cg_calls={} ui_calls={} role_calls={} keys={} fallback={}", self.calls, self.class_calls[0], self.class_calls[1], self.role_calls, self.keys.len(), self.fallback);
        }
    }
}
fn word(env: &Environment, address: u32) -> Option<u32> {
    Some(u32::from_le_bytes(env.mem.get_bytes_fallible(ConstVoidPtr::from_bits(address), 4)?.try_into().ok()?))
}

/// The verified CGFont/UIFont ID getters and their three glyph-cache consumers.
/// The original allocator's counter is shared with other native font classes;
/// reserve IDs through it rather than guessing an unused high-bit range.
pub(crate) fn prime(env: &mut Environment) {
    if env.framework_state.opengles.ds_font_roles.checked { return; }
    env.framework_state.opengles.ds_font_roles.checked = true;
    if !cfg!(target_os = "android") || env.bundle.bundle_identifier() != "com.ea.deadspace.bv"
        || std::env::var("TOUCHHLE_DS_FONT_ROLES").as_deref() == Ok("0") { return; }
    let fingerprint = [
        // Verified LayerMainMenu state/draw and LayerGameWorld draw entries.
        (MENU_STATE, 0xe92d_40f0), (MENU_STATE + 4, 0xe28d_700c),
        (MENU_STATE + 8, 0xe92d_0d00),
        (MENU_DRAW, 0xe92d_40f0), (MENU_DRAW + 4, 0xe28d_700c),
        (MENU_DRAW + 8, 0xe92d_0d00),
        (WORLD_DRAW, 0xe92d_40f0), (WORLD_DRAW + 4, 0xe28d_700c),
        (WORLD_DRAW + 8, 0xe92d_0d00),
        // GetHud proves the LayerGameWorld+0x250 embedded HUD offset.
        (0x0003_0474, 0xe92d_4080), (0x0003_0478, 0xe1a0_700d),
        (0x0003_047c, 0xebff_6c81), (0x0003_0480, 0xebff_4da5),
        (0x0003_0484, 0xe590_0000), (0x0003_0488, 0xe280_0e25),
        (0x0003_048c, 0xe8bd_8080),
        (GET_ID, 0xe590_0010), (GET_ID + 4, 0xe12f_ff1e),
        (CG_GET_ID, 0xe590_0008), (CG_GET_ID + 4, 0xe12f_ff1e),
        // FontCGFont's constructor reserves this same native ID and stores it
        // at +8; Chinese glyph fallback still rasterizes through UIKit.
        (0x0038_91e4, 0xeb03_baf5), (0x0038_91f0, 0xe581_0008),
        (0x0056_a650, 0xe12f_ff32), (0x0056_a654, 0xe58d_0000),
        (0x0056_b140, 0xe12f_ff31), (0x0056_b144, 0xe58d_0004),
        (0x0056_b1f8, 0xe12f_ff31), (0x0056_b1fc, 0xe58d_0000),
        (0x0047_7dc0, 0xe306_1fc0), (0x0047_7dc4, 0xe340_1067),
        (0x0047_7dc8, 0xe08f_1001), (0x0047_7dcc, 0xe591_0004),
        (0x0047_7dd0, 0xe280_0001), (0x0047_7dd4, 0xe581_0004),
        (0x0047_7dd8, 0xe12f_ff1e),
    ];
    if !fingerprint.iter().all(|&(at, op)| word(env, at) == Some(op)) || word(env, ID_COUNTER).is_none() {
        echo!("[DSfont-key] unsupported native cache; native role fonts withheld");
        return;
    }
    for address in [GET_ID, CG_GET_ID, MENU_STATE, MENU_DRAW, WORLD_DRAW] {
        env.mem.write(MutPtr::<u32>::from_bits(address), 0xe120_0070);
        env.cpu.invalidate_cache_range(address, 4);
    }
    let state = &mut env.framework_state.opengles.ds_font_roles;
    state.armed = true;
    state.trace = std::env::var("TOUCHHLE_DS_FONT_ROLE_TRACE").as_deref() == Ok("1");
    echo!("[DSfont-key] verified CGFont/UIFont role cache armed; native allocator retained");
}

fn return_id(cpu: &mut Cpu, id: u32) {
    let caller = cpu.regs()[Cpu::LR];
    cpu.regs_mut()[0] = id;
    cpu.branch(GuestFunction::from_addr_with_thumb_bit(caller));
}

pub(crate) fn handle_breakpoint(env: &mut Environment) -> bool {
    let pc = env.cpu.regs()[Cpu::PC];
    if !env.framework_state.opengles.ds_font_roles.armed { return false; }
    if handle_scene(env, pc) { return true; }
    if ![GET_ID, CG_GET_ID].contains(&pc) { return false; }
    let object = env.cpu.regs()[0];
    let caller = env.cpu.regs()[Cpu::LR];
    let offset = if pc == CG_GET_ID { 8 } else { 0x10 };
    // Replay the verified LDR r0,[r0,#8/#0x10]; BX lr, preserving the load's fault
    // behavior and every other register/flag. No guest function is called here.
    let original: u32 = env.mem.read(ConstPtr::from_bits(object.wrapping_add(offset)));
    let role = native_role(env).filter(|_| matches!(caller, 0x0056_a654 | 0x0056_b144 | 0x0056_b1fc));
    let mut result = original;
    env.framework_state.opengles.ds_font_roles.calls += 1;
    env.framework_state.opengles.ds_font_roles.class_calls[usize::from(pc == GET_ID)] += 1;
    if let Some(role) = role {
        let mut counter: u32 = env.mem.read(ConstPtr::from_bits(ID_COUNTER));
        let previous = counter;
        let state = &mut env.framework_state.opengles.ds_font_roles;
        state.role_calls += 1;
        state.face_calls[usize::from(role == Role::Story)] += 1;
        if let Some(key) = state.key(original, role, &mut counter) {
            result = key;
            if counter != previous {
                env.mem.write(MutPtr::<u32>::from_bits(ID_COUNTER), counter);
                if state.trace && state.keys.len() <= 32 {
                    echo!("[DSfont-key] reserve original={original} role={role:?} key={key}");
                }
            }
        } else {
            state.fallback += 1;
            if state.fallback == 1 { echo!("[DSfont-key] key capacity/domain exhausted; original native key retained"); }
        }
    }
    return_id(&mut env.cpu, result);
    true
}

pub(crate) fn native_role(env: &Environment) -> Option<Role> {
    let state = &env.framework_state.opengles.ds_font_roles;
    if cfg!(target_os = "android") && state.armed {
        Some(if state.scene_story { Role::Story } else { Role::Frontend })
    } else { None }
}

// All three verified scene entries start with PUSH {r4,r5,r6,r7,lr}.
// Replay only that instruction, with no guest call or layout/input mutation.
fn replay_scene_push(cpu: &mut Cpu, mem: &mut crate::mem::Mem) {
    let regs = *cpu.regs();
    let sp = regs[Cpu::SP].wrapping_sub(20);
    for (index, reg) in [4, 5, 6, 7, Cpu::LR].into_iter().enumerate() {
        mem.write(MutPtr::<u32>::from_bits(sp.wrapping_add(index as u32 * 4)), regs[reg]);
    }
    cpu.regs_mut()[Cpu::SP] = sp;
    cpu.regs_mut()[Cpu::PC] = regs[Cpu::PC] + 4;
}

fn handle_scene(env: &mut Environment, pc: u32) -> bool {
    let (slot, next) = match pc {
        MENU_STATE => (0, (2..=32).contains(&env.cpu.regs()[1]).then_some(Role::Frontend)),
        MENU_DRAW => (1, Some(Role::Frontend)),
        WORLD_DRAW => {
            let layer = env.cpu.regs()[0];
            let world = word(env, layer.saturating_add(0x254)).unwrap_or(0);
            let player = if world >= 0x1000 { word(env, world.saturating_add(0xe0)).unwrap_or(0) } else { 0 };
            let live_world = player >= 0x1000 && word(env, player) == Some(0x00a7_a4f8);
            (2, live_world.then_some(Role::Story))
        }
        _ => return false,
    };
    let state = &mut env.framework_state.opengles.ds_font_roles;
    state.scene_calls[slot] += 1;
    state.observe_scene(next);
    replay_scene_push(&mut env.cpu, &mut env.mem);
    true
}

pub(crate) fn load(role: Role) -> Font {
    let (bytes, name) = match role {
        Role::Frontend => (TITLE, "MStiffHei"),
        Role::Story => (STORY, "SweiShearSansCJKsc-Regular"),
    };
    echo!("[DSfont-role] loaded role={role:?} face={name} bytes={}", bytes.len());
    Font::from_vec(bytes.to_vec()).expect("verified Dead Space role font")
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::{cpu::CpuState, mem::Mem};

    #[test]
    fn drawing_owners_keep_story_across_missing_hud_and_restore_frontend() {
        let mut state = State::default();
        assert!(!state.scene_story);
        state.observe_scene(Some(Role::Frontend));
        state.observe_scene(Some(Role::Story));
        assert!(state.scene_story);
        state.observe_scene(None); // paused/modal or temporarily unavailable world
        assert!(state.scene_story);
        state.observe_scene(Some(Role::Frontend));
        assert!(!state.scene_story);
        state.observe_scene(None); // invalid world cannot take frontend ownership
        assert!(!state.scene_story);
        assert_eq!(state.scene_changes, 2);
    }

    #[test]
    fn scene_entry_replay_matches_original_push_registers_flags_stack_and_fpscr() {
        let mut mem = Mem::new();
        mem.set_null_segment_size(0x1000);
        mem.write(MutPtr::<u32>::from_bits(0x2000), 0xe92d_40f0);
        mem.write(MutPtr::<u32>::from_bits(0x2004), 0xef00_0001);
        let mut cpu = Cpu::new(None);
        for caller in [0x3000, 0x4001] {
            for fpscr in [0, 0x0100_009f] {
                for cpsr in [0x10, 0xf800_0010] {
                    let mut initial = std::array::from_fn(|i| 0x1111_0000 + i as u32);
                    initial[Cpu::SP] = 0x8000;
                    initial[Cpu::LR] = caller;
                    initial[Cpu::PC] = 0x2000;
                    let reset = |cpu: &mut Cpu, mem: &mut Mem| {
                        for address in (0x7fe0..=0x8008).step_by(4) {
                            mem.write(MutPtr::<u32>::from_bits(address), 0xcafe_f00d);
                        }
                        *cpu.regs_mut() = initial;
                        cpu.set_cpsr(cpsr);
                        cpu.set_fpscr(fpscr);
                    };
                    reset(&mut cpu, &mut mem);
                    let mut ticks = 1000;
                    assert!(matches!(cpu.run_or_step(&mut mem, Some(&mut ticks)), CpuState::Svc(1)));
                    let expected = (*cpu.regs(), cpu.cpsr(), cpu.fpscr());
                    let stack: Vec<u32> = (0x7fe0..=0x8008).step_by(4)
                        .map(|at| mem.read(ConstPtr::from_bits(at))).collect();
                    reset(&mut cpu, &mut mem);
                    replay_scene_push(&mut cpu, &mut mem);
                    let mut ticks = 1000;
                    assert!(matches!(cpu.run_or_step(&mut mem, Some(&mut ticks)), CpuState::Svc(1)));
                    assert_eq!((*cpu.regs(), cpu.cpsr(), cpu.fpscr()), expected);
                    let actual: Vec<u32> = (0x7fe0..=0x8008).step_by(4)
                        .map(|at| mem.read(ConstPtr::from_bits(at))).collect();
                    assert_eq!(actual, stack);
                }
            }
        }
    }

    #[test]
    fn cache_ids_share_native_allocator_without_aliasing_original_or_other_roles() {
        let mut state = State::default();
        let mut counter = 17;
        assert_eq!(state.key(7, Role::Frontend, &mut counter), Some(18));
        assert_eq!(state.key(7, Role::Story, &mut counter), Some(19));
        assert_eq!(state.key(7, Role::Frontend, &mut counter), Some(18));
        assert_eq!(counter, 19);
        // Simulate the next original native font allocation, interleaved with
        // role-cache allocations. It cannot reuse either reserved cache ID.
        counter += 1;
        assert_eq!(state.key(counter, Role::Frontend, &mut counter), Some(21));
        assert_eq!(state.key(7, Role::Story, &mut counter), Some(19));
        assert_eq!(counter, 21);
    }

    #[test]
    fn cache_capacity_invalid_id_and_counter_overflow_do_not_write_counter() {
        let mut state = State::default();
        let mut counter = 1000;
        assert_eq!(state.key(0, Role::Story, &mut counter), None);
        assert_eq!(state.key(1001, Role::Story, &mut counter), None);
        assert_eq!(counter, 1000);
        for original in 1..=MAX_KEYS as u32 {
            assert!(state.key(original, Role::Frontend, &mut counter).is_some());
        }
        let before = counter;
        assert_eq!(state.key(1, Role::Story, &mut counter), None);
        assert_eq!(counter, before);
        assert_eq!(state.key(1, Role::Frontend, &mut counter), Some(1001));
        let mut state = State::default();
        let mut counter = u32::MAX;
        assert_eq!(state.key(1, Role::Story, &mut counter), None);
        assert_eq!(counter, u32::MAX);
        assert!(state.keys.is_empty());
    }

    #[test]
    fn id_return_preserves_arm_and_thumb_caller_gprs_flags_stack_and_fpscr() {
        let mut mem = Mem::new();
        mem.set_null_segment_size(0x1000);
        mem.write(MutPtr::<u32>::from_bits(0x2004), 0xe12f_ff1e); // original BX LR
        mem.write(MutPtr::<u32>::from_bits(0x3000), 0xef00_0001); // ARM caller sentinel
        mem.write(MutPtr::<u16>::from_bits(0x4000), 0xdf01); // Thumb caller sentinel
        mem.write(MutPtr::<u32>::from_bits(0x5010), 17);
        mem.write(MutPtr::<u32>::from_bits(0x5008), 17);
        let mut cpu = Cpu::new(None);
        for original_load in [0xe590_0008, 0xe590_0010] {
            mem.write(MutPtr::<u32>::from_bits(0x2000), original_load);
            cpu.invalidate_cache_range(0x2000, 4);
            for caller in [0x3000, 0x4001] {
                for fpscr in [0, 0x0100_009f] {
                    let mut initial = std::array::from_fn(|i| 0x1111_0000 + i as u32);
                    initial[0] = 0x5000;
                    initial[Cpu::SP] = 0x8000;
                    initial[Cpu::LR] = caller;
                    initial[Cpu::PC] = 0x2000;
                    *cpu.regs_mut() = initial;
                    cpu.set_cpsr(0xa000_0010);
                    cpu.set_fpscr(fpscr);
                    let mut ticks = 1000;
                    assert!(matches!(cpu.run_or_step(&mut mem, Some(&mut ticks)), CpuState::Svc(1)));
                    let expected = (*cpu.regs(), cpu.cpsr(), cpu.fpscr());
                    for result in [17, 18, 19] {
                        *cpu.regs_mut() = initial;
                        cpu.set_cpsr(0xa000_0010);
                        cpu.set_fpscr(fpscr);
                        return_id(&mut cpu, result);
                        let mut ticks = 1000;
                        assert!(matches!(cpu.run_or_step(&mut mem, Some(&mut ticks)), CpuState::Svc(1)));
                        let mut registers = expected.0;
                        registers[0] = result;
                        assert_eq!(*cpu.regs(), registers);
                        assert_eq!(cpu.cpsr(), expected.1);
                        assert_eq!(cpu.fpscr(), expected.2);
                    }
                }
            }
        }
    }
}
