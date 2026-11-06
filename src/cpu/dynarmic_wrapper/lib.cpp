/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
#include <cstdint>
#include <cstdio>
#if defined(__ANDROID__)
#include <android/log.h>
#include <cerrno>
#include <cstdlib>
#include <cstring>
#include <sys/mman.h>
#include <unistd.h>
#endif

#include "dynarmic/interface/A32/a32.h"
#include "dynarmic/interface/A32/config.h"
#include "dynarmic/interface/A32/coprocessor.h"
#include "dynarmic/interface/exclusive_monitor.h"

namespace touchHLE::cpu {

using VAddr = std::uint32_t;

// Types and functions defined in Rust
extern "C" {
struct touchHLE_Mem;
std::uint8_t touchHLE_cpu_read_u8(touchHLE_Mem *mem, VAddr addr, bool *error);
std::uint16_t touchHLE_cpu_read_u16(touchHLE_Mem *mem, VAddr addr, bool *error);
std::uint32_t touchHLE_cpu_read_u32(touchHLE_Mem *mem, VAddr addr, bool *error);
std::uint64_t touchHLE_cpu_read_u64(touchHLE_Mem *mem, VAddr addr, bool *error);
bool touchHLE_cpu_write_u8(touchHLE_Mem *mem, VAddr addr, std::uint8_t value);
bool touchHLE_cpu_write_u16(touchHLE_Mem *mem, VAddr addr, std::uint16_t value);
bool touchHLE_cpu_write_u32(touchHLE_Mem *mem, VAddr addr, std::uint32_t value);
bool touchHLE_cpu_write_u64(touchHLE_Mem *mem, VAddr addr, std::uint64_t value);
struct touchHLE_DynarmicContext {
  std::array<std::uint32_t, 16> regs;
  std::array<std::uint32_t, 64> extregs;
  std::uint32_t cpsr;
  std::uint32_t fpscr;
};
}

const auto HaltReasonSvc = Dynarmic::HaltReason::UserDefined1;
const auto HaltReasonUndefinedInstruction = Dynarmic::HaltReason::UserDefined2;
const auto HaltReasonBreakpoint = Dynarmic::HaltReason::UserDefined3;
// ★ An instruction whose behaviour the ARM ARM leaves "unpredictable", or a
// hint instruction this wrapper does not implement. Real hardware does
// *something* here; dynarmic refuses to guess. Aborting the whole emulator on
// these is much worse than continuing: measured 2026-10-17, a save-state
// restore left Dead Space executing one and the process died with
// STATUS_STACK_BUFFER_OVERRUN ("ExceptionRaised: unexpected exception 1"),
// killing an otherwise healthy session. They now halt with a recoverable
// reason so the Rust layer's existing bypass (log + fake return to LR) runs.
const auto HaltReasonUnpredictable = Dynarmic::HaltReason::UserDefined4;

class Environment final : public Dynarmic::A32::UserCallbacks {
public:
  Dynarmic::A32::Jit *cpu = nullptr;
  touchHLE_Mem *mem = nullptr;
  std::uint64_t ticks_remaining;
  uint32_t halting_svc;

private:
  std::uint8_t MemoryRead8(VAddr vaddr) override {
    bool error;
    auto value = touchHLE_cpu_read_u8(mem, vaddr, &error);
    if (error) {
      cpu->HaltExecution(Dynarmic::HaltReason::MemoryAbort);
    }
    return value;
  }
  std::uint16_t MemoryRead16(VAddr vaddr) override {
    bool error;
    auto value = touchHLE_cpu_read_u16(mem, vaddr, &error);
    if (error) {
      cpu->HaltExecution(Dynarmic::HaltReason::MemoryAbort);
    }
    return value;
  }
  std::uint32_t MemoryRead32(VAddr vaddr) override {
    bool error;
    auto value = touchHLE_cpu_read_u32(mem, vaddr, &error);
    if (error) {
      cpu->HaltExecution(Dynarmic::HaltReason::MemoryAbort);
    }
    return value;
  }
  std::uint64_t MemoryRead64(VAddr vaddr) override {
    bool error;
    auto value = touchHLE_cpu_read_u64(mem, vaddr, &error);
    if (error) {
      cpu->HaltExecution(Dynarmic::HaltReason::MemoryAbort);
    }
    return value;
  }

  std::optional<std::uint32_t> MemoryReadCode(VAddr vaddr) override {
    bool error;
    auto value = touchHLE_cpu_read_u32(mem, vaddr, &error);
    if (error) {
      return std::nullopt;
    } else {
      return value;
    }
  }

  void MemoryWrite8(VAddr vaddr, std::uint8_t value) override {
    if (touchHLE_cpu_write_u8(mem, vaddr, value)) {
      cpu->HaltExecution(Dynarmic::HaltReason::MemoryAbort);
    }
  }
  void MemoryWrite16(VAddr vaddr, std::uint16_t value) override {
    if (touchHLE_cpu_write_u16(mem, vaddr, value)) {
      cpu->HaltExecution(Dynarmic::HaltReason::MemoryAbort);
    }
  }
  void MemoryWrite32(VAddr vaddr, std::uint32_t value) override {
    if (touchHLE_cpu_write_u32(mem, vaddr, value)) {
      cpu->HaltExecution(Dynarmic::HaltReason::MemoryAbort);
    }
  }
  void MemoryWrite64(VAddr vaddr, std::uint64_t value) override {
    if (touchHLE_cpu_write_u64(mem, vaddr, value)) {
      cpu->HaltExecution(Dynarmic::HaltReason::MemoryAbort);
    }
  }

  bool MemoryWriteExclusive8(VAddr addr, std::uint8_t value,
                             std::uint8_t expected) override {
    if (MemoryRead8(addr) != expected) {
      return false;
    }
    MemoryWrite8(addr, value);
    return true;
  }
  bool MemoryWriteExclusive16(VAddr addr, std::uint16_t value,
                              std::uint16_t expected) override {
    if (MemoryRead16(addr) != expected) {
      return false;
    }
    MemoryWrite16(addr, value);
    return true;
  }
  bool MemoryWriteExclusive32(VAddr addr, std::uint32_t value,
                              std::uint32_t expected) override {
    if (MemoryRead32(addr) != expected) {
      return false;
    }
    MemoryWrite32(addr, value);
    return true;
  }
  bool MemoryWriteExclusive64(VAddr addr, std::uint64_t value,
                              std::uint64_t expected) override {
    if (MemoryRead64(addr) != expected) {
      return false;
    }
    MemoryWrite64(addr, value);
    return true;
  }

  void InterpreterFallback(std::uint32_t, size_t) override {
    abort(); // TODO
  }
  void CallSVC(std::uint32_t svc) override {
    halting_svc = svc;
    cpu->HaltExecution(HaltReasonSvc);
  }
  void ExceptionRaised(VAddr pc, Dynarmic::A32::Exception exception) override {
    // MemoryReadCode returned nullopt
    if (exception == Dynarmic::A32::Exception::NoExecuteFault) {
      cpu->HaltExecution(Dynarmic::HaltReason::MemoryAbort);
    } else if (exception == Dynarmic::A32::Exception::UndefinedInstruction) {
      cpu->HaltExecution(HaltReasonUndefinedInstruction);
    } else if (exception == Dynarmic::A32::Exception::Breakpoint) {
      cpu->HaltExecution(HaltReasonBreakpoint);
    } else if (exception == Dynarmic::A32::Exception::DecodeError) {
      // A real decoder bug - keep failing loudly.
      std::fprintf(stderr, "ExceptionRaised: decode error at %x\n", pc);
      abort();
    } else {
      // UnpredictableInstruction, or a hint instruction (SEV/SEVL/WFI/WFE/
      // YIELD/PLD/PLI) that this wrapper does not implement. Log the kind once
      // per boot and let the Rust layer bypass it.
      static std::uint64_t seen = 0;
      const std::uint64_t bit = 1ull << (unsigned(exception) & 63u);
      if ((seen & bit) == 0) {
        seen |= bit;
        std::fprintf(stderr,
                     "ExceptionRaised: unhandled exception %u at %x - treating "
                     "it as a recoverable undefined instruction\n",
                     unsigned(exception), pc);
      }
      cpu->HaltExecution(HaltReasonUnpredictable);
    }
  }
  void AddTicks(std::uint64_t ticks) override {
    if (ticks > ticks_remaining) {
      ticks_remaining = 0;
      return;
    }
    ticks_remaining -= ticks;
  }
  std::uint64_t GetTicksRemaining() override { return ticks_remaining; }
};

class ArmDynarmicCP15 : public Dynarmic::A32::Coprocessor {
  static std::uint64_t Ignore(void *, std::uint32_t, std::uint32_t) {
    return 0;
  }

  static Callback CallbackForIgnoredOperation() {
    return Callback{&Ignore, std::nullopt};
  }

public:
  using CoprocReg = Dynarmic::A32::CoprocReg;

  CallbackOrAccessOneWord CompileSendOneWord(bool, unsigned, CoprocReg,
                                             CoprocReg, unsigned) override {
    return CallbackForIgnoredOperation();
  }

  std::optional<Callback> CompileInternalOperation(bool, unsigned, CoprocReg,
                                                   CoprocReg, CoprocReg,
                                                   unsigned) override {
    return CallbackForIgnoredOperation();
  }

  CallbackOrAccessTwoWords CompileSendTwoWords(bool, unsigned, CoprocReg) override {
    return CallbackForIgnoredOperation();
  }

  CallbackOrAccessOneWord CompileGetOneWord(bool, unsigned, CoprocReg,
                                            CoprocReg, unsigned) override {
    return CallbackForIgnoredOperation();
  }

  CallbackOrAccessTwoWords CompileGetTwoWords(bool, unsigned, CoprocReg) override {
    return CallbackForIgnoredOperation();
  }

  std::optional<Callback> CompileLoadWords(bool, bool, CoprocReg,
                                           std::optional<std::uint8_t>) override {
    return CallbackForIgnoredOperation();
  }

  std::optional<Callback> CompileStoreWords(bool, bool, CoprocReg,
                                            std::optional<std::uint8_t>) override {
    return CallbackForIgnoredOperation();
  }
};

class DynarmicWrapper {
  Environment env;
  std::unique_ptr<Dynarmic::A32::Jit> cpu;
  std::unique_ptr<Dynarmic::ExclusiveMonitor> mon;
  std::array<std::uint8_t *, Dynarmic::A32::UserConfig::NUM_PAGE_TABLE_ENTRIES>
      page_table;
#if defined(__ANDROID__)
  void *fastmem_guard = nullptr;
  size_t fastmem_guard_size = 0;
#endif

public:
  DynarmicWrapper(void *direct_memory_access_ptr, size_t null_page_count) {
    Dynarmic::A32::UserConfig user_config;
    user_config.callbacks = &env;
    user_config.coprocessors[15] = std::make_shared<ArmDynarmicCP15>();
    mon = std::make_unique<Dynarmic::ExclusiveMonitor>(1);
    user_config.global_monitor = mon.get();
#ifndef NDEBUG
    user_config.check_halt_on_memory_access = true;
#endif
    if (direct_memory_access_ptr) {
      // Page-table accesses to the null segment use touchHLE's compatibility
      // callbacks. The optional Android fastmem path below retains this table
      // and protects the same null segment before enabling direct JIT access.
      page_table.fill((std::uint8_t *)direct_memory_access_ptr);
      // Note that the null page size is also defined in src/mem.rs.
      static_assert(1 << Dynarmic::A32::UserConfig::PAGE_BITS == 0x1000);

      if (null_page_count > page_table.size()) {
        printf("Too many null pages, %zu requested but maximum is %zu.",
               null_page_count, page_table.size());
        abort();
      }
      for (size_t i = 0; i < null_page_count; i++) {
        page_table[i] = nullptr;
      }
      user_config.page_table = &page_table;
      user_config.absolute_offset_page_table = true;
    }
#if defined(__ANDROID__)
    // Opt-in only. Keep the original page table as Dynarmic's fault fallback,
    // including touchHLE's zero-read / absorbed-write null-segment callbacks.
    // Never enable fastmem over an accessible null segment: that would let a
    // guest write change later null reads and bypass the compatibility shim.
    const char *requested = std::getenv("TOUCHHLE_DS_CPU_FASTMEM");
    const bool want_fastmem = requested && std::strcmp(requested, "1") == 0;
    const long host_page_size = sysconf(_SC_PAGESIZE);
    const size_t guard_size = null_page_count * size_t{0x1000};
    const bool guard_shape_ok = sizeof(void *) == 8 && direct_memory_access_ptr
        && guard_size != 0 && host_page_size > 0
        && guard_size % static_cast<size_t>(host_page_size) == 0
        && reinterpret_cast<uintptr_t>(direct_memory_access_ptr)
               % static_cast<size_t>(host_page_size) == 0;
    if (want_fastmem && guard_shape_ok
        && mprotect(direct_memory_access_ptr, guard_size, PROT_NONE) == 0) {
      fastmem_guard = direct_memory_access_ptr;
      fastmem_guard_size = guard_size;
      user_config.fastmem_pointer =
          reinterpret_cast<uintptr_t>(direct_memory_access_ptr);
      // Safe optimizations and exclusive-access behavior remain unchanged.
      __android_log_print(ANDROID_LOG_INFO, "touchHLE",
          "[DSjit-fastmem] requested=1 effective=fastmem guard_bytes=%zu host_page=%ld fallback=page-table",
          guard_size, host_page_size);
    } else {
      __android_log_print(ANDROID_LOG_INFO, "touchHLE",
          "[DSjit-fastmem] requested=%d effective=page-table guard_shape_ok=%d host_page=%ld guard_bytes=%zu errno=%d",
          want_fastmem, guard_shape_ok, host_page_size, guard_size,
          want_fastmem && guard_shape_ok ? errno : 0);
    }
#endif
    cpu = std::make_unique<Dynarmic::A32::Jit>(user_config);
    env.cpu = cpu.get();
  }

  ~DynarmicWrapper() {
    cpu.reset(); // No cached JIT code can access memory after restoring it.
#if defined(__ANDROID__)
    if (fastmem_guard && mprotect(fastmem_guard, fastmem_guard_size,
                                  PROT_READ | PROT_WRITE) != 0) {
      __android_log_print(ANDROID_LOG_ERROR, "touchHLE",
          "[DSjit-fastmem] null guard restore failed errno=%d", errno);
    }
#endif
  }

  const std::uint32_t *regs() const { return &cpu->Regs().front(); }
  std::uint32_t *regs() { return &cpu->Regs().front(); }

  std::uint32_t cpsr() const { return cpu->Cpsr(); }
  std::uint32_t fpscr() const { return cpu->Fpscr(); }
  void set_fpscr(std::uint32_t value) { cpu->SetFpscr(value); }
  void set_cpsr(std::uint32_t cpsr) { cpu->SetCpsr(cpsr); }

  void invalidate_cache_range(VAddr start, std::uint32_t size) {
    cpu->InvalidateCacheRange(start, size);
  }

  void swap_context(touchHLE_DynarmicContext *context) {
    touchHLE_DynarmicContext tmp = {cpu->Regs(), cpu->ExtRegs(), cpu->Cpsr(),
                                    cpu->Fpscr()};
    cpu->Regs() = context->regs;
    cpu->ExtRegs() = context->extregs;
    cpu->SetCpsr(context->cpsr);
    cpu->SetFpscr(context->fpscr);
    *context = tmp;
  }

  std::int32_t run_or_step(touchHLE_Mem *mem, std::uint64_t *ticks) {
    env.mem = mem;
    Dynarmic::HaltReason hr;
    if (ticks) {
      env.ticks_remaining = *ticks;
      hr = cpu->Run();
    } else {
      hr = cpu->Step();
    }
    std::int32_t res;
    if ((!hr && ticks) || (hr == Dynarmic::HaltReason::Step && !ticks)) {
      res = -1;
    } else if (Dynarmic::Has(hr, Dynarmic::HaltReason::MemoryAbort)) {
      res = -2;
    } else if (Dynarmic::Has(hr, HaltReasonUndefinedInstruction)) {
      res = -3;
    } else if (Dynarmic::Has(hr, HaltReasonBreakpoint)) {
      res = -4;
    } else if (Dynarmic::Has(hr, HaltReasonUnpredictable)) {
      res = -5;
    } else if (Dynarmic::Has(hr, HaltReasonSvc)) {
      res = std::int32_t(env.halting_svc);
    } else {
      printf("unhandled halt reason %u\n", unsigned(hr));
      abort();
    }
    env.mem = nullptr;
    if (ticks) {
      *ticks = env.ticks_remaining;
    }
    return res;
  }
};

extern "C" {

DynarmicWrapper *touchHLE_DynarmicWrapper_new(void *direct_memory_access_ptr,
                                              size_t null_page_count) {
  return new DynarmicWrapper(direct_memory_access_ptr, null_page_count);
}
void touchHLE_DynarmicWrapper_delete(DynarmicWrapper *cpu) { delete cpu; }

const std::uint32_t *
touchHLE_DynarmicWrapper_regs_const(const DynarmicWrapper *cpu) {
  return cpu->regs();
}
std::uint32_t *touchHLE_DynarmicWrapper_regs_mut(DynarmicWrapper *cpu) {
  return cpu->regs();
}

std::uint32_t touchHLE_DynarmicWrapper_cpsr(const DynarmicWrapper *cpu) {
  return cpu->cpsr();
}
std::uint32_t touchHLE_DynarmicWrapper_fpscr(const DynarmicWrapper *cpu) {
  return cpu->fpscr();
}
void touchHLE_DynarmicWrapper_set_fpscr(DynarmicWrapper *cpu, std::uint32_t value) {
  cpu->set_fpscr(value);
}
void touchHLE_DynarmicWrapper_set_cpsr(DynarmicWrapper *cpu,
                                       std::uint32_t cpsr) {
  cpu->set_cpsr(cpsr);
}

void touchHLE_DynarmicWrapper_swap_context(DynarmicWrapper *cpu,
                                           touchHLE_DynarmicContext *context) {
  cpu->swap_context(context);
}

void touchHLE_DynarmicWrapper_invalidate_cache_range(DynarmicWrapper *cpu,
                                                     VAddr start,
                                                     std::uint32_t size) {
  cpu->invalidate_cache_range(start, size);
}

std::int32_t touchHLE_DynarmicWrapper_run_or_step(DynarmicWrapper *cpu,
                                                  touchHLE_Mem *mem,
                                                  std::uint64_t *ticks) {
  return cpu->run_or_step(mem, ticks);
}
}

} // namespace touchHLE::cpu
