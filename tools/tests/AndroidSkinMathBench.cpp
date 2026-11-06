// Offline-component experiment using byte-exact code copied from the original
// IPA. No game process, GL, application data or guest Environment is shared
// with workers.
#include "lib.cpp"
#include <arm_neon.h>
#include <chrono>
#include <memory>
#include <random>
#include <sys/mman.h>
#include <unistd.h>
#include <vector>
namespace touchHLE::cpu {
struct touchHLE_Mem {
  unsigned char *p;
};
template <class T> T rd(touchHLE_Mem *m, uint32_t a, bool *e) {
  *e = false;
  if (a < 4096)
    return 0;
  T v;
  memcpy(&v, m->p + a, sizeof(v));
  return v;
}
template <class T> bool wr(touchHLE_Mem *m, uint32_t a, T v) {
  if (a >= 4096)
    memcpy(m->p + a, &v, sizeof(v));
  return false;
}
extern "C" {
uint8_t touchHLE_cpu_read_u8(touchHLE_Mem *m, uint32_t a, bool *e) {
  return rd<uint8_t>(m, a, e);
}
uint16_t touchHLE_cpu_read_u16(touchHLE_Mem *m, uint32_t a, bool *e) {
  return rd<uint16_t>(m, a, e);
}
uint32_t touchHLE_cpu_read_u32(touchHLE_Mem *m, uint32_t a, bool *e) {
  return rd<uint32_t>(m, a, e);
}
uint64_t touchHLE_cpu_read_u64(touchHLE_Mem *m, uint32_t a, bool *e) {
  return rd<uint64_t>(m, a, e);
}
bool touchHLE_cpu_write_u8(touchHLE_Mem *m, uint32_t a, uint8_t v) {
  return wr(m, a, v);
}
bool touchHLE_cpu_write_u16(touchHLE_Mem *m, uint32_t a, uint16_t v) {
  return wr(m, a, v);
}
bool touchHLE_cpu_write_u32(touchHLE_Mem *m, uint32_t a, uint32_t v) {
  return wr(m, a, v);
}
bool touchHLE_cpu_write_u64(touchHLE_Mem *m, uint32_t a, uint64_t v) {
  return wr(m, a, v);
}
}
} // namespace touchHLE::cpu
using namespace touchHLE::cpu;
constexpr uint32_t POS = 0x31000000, IDX = 0x32000000, WGT = 0x33000000,
                   MAT = 0x34000000, DEST = 0x35000000;
extern "C" uint32_t touchHLE_skin_math(float *, const float *, const float *,
                                       const int8_t *, const float *, uint32_t,
                                       uint32_t);
uint32_t host_skin(unsigned char *p, unsigned first, unsigned count,
                   bool normal) {
  return touchHLE_skin_math((float *)(p + DEST) + 3 * first,
                            (const float *)(p + POS) + 3 * first,
                            (const float *)(p + WGT) + 4 * first,
                            (const int8_t *)(p + IDX) + 4 * first,
                            (const float *)(p + MAT), count, normal);
}

int main(int argc, char **argv) {
  if (argc != 2 || sysconf(_SC_PAGESIZE) != 4096)
    return 2;
  size_t size = size_t(1) << 32;
  auto *p = (unsigned char *)mmap(nullptr, size, PROT_READ | PROT_WRITE,
                                  MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
  if (p == MAP_FAILED)
    return 3;
  FILE *f = fopen(argv[1], "rb");
  if (!f)
    return 4;
  uint32_t n;
  if (fread(&n, 4, 1, f) != 1)
    return 5;
  for (unsigned i = 0; i < n; ++i) {
    uint32_t at, len;
    if (fread(&at, 4, 1, f) != 1 || fread(&len, 4, 1, f) != 1 ||
        fread(p + at, 1, len, f) != len)
      return 6;
  }
  fclose(f);
  auto put = [&](uint32_t at, uint32_t v) { memcpy(p + at, &v, 4); };
  const uint32_t objects[] = {0x30000000, 0x30000100, 0x30000200};
  const uint32_t bases[] = {POS, IDX, WGT};
  for (unsigned i = 0; i < 3; ++i) {
    put(objects[i] + 68, i == 0 ? 3 : 4);
    put(objects[i] + 76, bases[i]);
    put(objects[i] + 84, 0);
    put(objects[i] + 88, i == 0 ? 12 : (i == 1 ? 4 : 16));
  }
  put(0xabb844, objects[0]);
  put(0xabb848, objects[1]);
  put(0xabb84c, objects[2]);
  put(0xabb83c, 0x30000300);
  put(0x30000308, MAT);
  put(0x10000, 0xef000001);
  std::mt19937 rng(20261103);
  auto val = [&] { return float(int(rng() % 20001) - 10000) / 4096.0f; };
  auto init = [&](unsigned count, unsigned trial) {
    for (unsigned i = 0; i < 32 * 16; ++i)
      ((float *)(p + MAT))[i] = val();
    for (unsigned i = 0; i < count; ++i) {
      for (unsigned k = 0; k < 3; ++k) {
        ((float *)(p + POS))[3 * i + k] = val();
      }
      float w0 = float(1 + rng() % 1000) / 1000,
            w1 = float(rng() % 1001) / 1000, w2 = float(rng() % 1001) / 1000,
            w3 = float(rng() % 1001) / 1000;
      for (unsigned k = 0; k < 4; ++k)
        p[IDX + 4 * i + k] = rng() % 32;
      if (trial % 4 == 0) {
        w1 = 0;
        w2 = 0;
        w3 = 0;
      } else if (trial % 4 == 1) {
        w2 = 0;
        w3 = 0;
      } else if (trial % 4 == 2)
        w3 = 0;
      ((float *)(p + WGT))[4 * i] = w0;
      ((float *)(p + WGT))[4 * i + 1] = w1;
      ((float *)(p + WGT))[4 * i + 2] = w2;
      ((float *)(p + WGT))[4 * i + 3] = w3;
    }
  };
  touchHLE_Mem mem{p};
  setenv("TOUCHHLE_DS_CPU_FASTMEM", "1", 1);
  auto cpu = std::make_unique<DynarmicWrapper>(p, 1);
  auto fpscr = [&] {
    touchHLE_DynarmicContext c{};
    cpu->swap_context(&c);
    uint32_t f = c.fpscr;
    cpu->swap_context(&c);
    return f;
  };
  auto clear_fpscr = [&] {
    touchHLE_DynarmicContext c{};
    cpu->swap_context(&c);
    c.fpscr = 0;
    cpu->swap_context(&c);
  };
  auto original = [&](unsigned first, unsigned count, bool normal,
                      bool replay = false) {
    auto *r = cpu->regs();
    for (unsigned i = 0; i < 16; ++i)
      r[i] = 0;
    cpu->set_cpsr(0x10);
    r[0] = DEST;
    r[1] = first;
    r[2] = count;
    r[3] = 123;
    r[13] = 0x3fff0000;
    r[14] = 0x10000;
    r[15] = normal ? 0x37f814 : 0x37fac8;
    if (replay) {
      uint32_t push[] = {r[4], r[5], r[6], r[7], r[14]};
      r[13] -= 20;
      memcpy(p + r[13], push, 20);
      r[15] += 4;
    }
    uint64_t ticks = 100000000000ULL;
    int result = cpu->run_or_step(&mem, &ticks);
    if (result != 1) {
      fprintf(stderr, "Unexpected JIT exit %d\n", result);
      exit(7);
    }
  };
  for (unsigned trial = 0; trial < 120; ++trial) {
    unsigned first = trial % 5, count = 1 + rng() % 193;
    init(first + count + 2, trial);
    size_t bytes = (first + count + 2) * 12;
    std::vector<uint8_t> expected(bytes);
    for (bool normal : {false, true}) {
      memset(p + DEST, 0xcd, bytes);
      clear_fpscr();
      original(first, count, normal);
      uint32_t original_flags = fpscr() & 0x9f;
      memcpy(expected.data(), p + DEST, bytes);
      std::array<uint32_t, 16> full_regs;
      memcpy(full_regs.data(), cpu->regs(), sizeof(full_regs));
      uint32_t full_cpsr = cpu->cpsr();
      memset(p + DEST, 0xcd, bytes);
      clear_fpscr();
      original(first, count, normal, true);
      if (memcmp(expected.data(), p + DEST, bytes) ||
          memcmp(full_regs.data(), cpu->regs(), sizeof(full_regs)) ||
          cpu->cpsr() != full_cpsr || (fpscr() & 0x9f) != original_flags) {
        fprintf(stderr, "Prologue replay mismatch trial=%u\n", trial);
        return 11;
      }
      memset(p + DEST, 0xcd, bytes);
      uint32_t host_flags = host_skin(p, first, count, normal);
      if (original_flags != host_flags) {
        fprintf(stderr, "Flag mismatch trial=%u normal=%d arm=%x host=%x\n",
                trial, normal, original_flags, host_flags);
        return 9;
      }
      if (memcmp(expected.data(), p + DEST, bytes)) {
        unsigned off = 0;
        while (expected[off] == p[DEST + off])
          ++off;
        fprintf(stderr,
                "Mismatch trial=%u normal=%d byte=%u original=%u host=%u\n",
                trial, normal, off, expected[off], p[DEST + off]);
        return 8;
      }
    }
  }
  printf(
      "{\"original_arm_functions\":2,\"byte_equal_cases\":240,\"fpscr_flags_"
      "equal\":true,\"prologue_replay_equal\":true,\"component_only\":true}\n");
  fflush(stdout);

  const uint32_t patterns[] = {0,          0x80000000, 1,          0x80000001,
                               0x007fffff, 0x807fffff, 0x00800000, 0x80800000,
                               0x7f7fffff, 0xff7fffff, 0x7f800000, 0xff800000,
                               0x7fc12345, 0xffc12345, 0x7fa12345, 0xffa12345};
  unsigned edge_count = 0;
  for (unsigned pattern : patterns)
    for (unsigned placement = 0; placement < 4; ++placement)
      for (bool normal : {false, true}) {
        init(4, 0);
        ((float *)(p + WGT))[0] = 1;
        ((float *)(p + WGT))[1] = 0;
        ((float *)(p + WGT))[2] = 0;
        ((float *)(p + WGT))[3] = 0;
        uint32_t at =
            placement == 0
                ? POS
                : (placement == 1 ? MAT : (placement == 2 ? WGT : WGT + 4));
        memcpy(p + at, &pattern, 4);
        if (placement == 1)
          memset(p + IDX, 0, 4);
        memset(p + DEST, 0xcd, 48);
        clear_fpscr();
        original(0, 1, normal);
        uint32_t original_flags = fpscr() & 0x9f;
        std::array<uint8_t, 48> expected;
        memcpy(expected.data(), p + DEST, 48);
        memset(p + DEST, 0xcd, 48);
        clear_fpscr();
        original(0, 1, normal, true);
        if (memcmp(expected.data(), p + DEST, 48) ||
            (fpscr() & 0x9f) != original_flags) {
          fprintf(stderr, "Edge replay mismatch\n");
          return 12;
        }
        memset(p + DEST, 0xcd, 48);
        uint32_t host_flags = host_skin(p, 0, 1, normal);
        if (original_flags != host_flags ||
            memcmp(expected.data(), p + DEST, 48)) {
          fprintf(
              stderr,
              "Edge mismatch pattern=%08x placement=%u normal=%d flags=%x/%x\n",
              pattern, placement, normal, original_flags, host_flags);
          return 10;
        }
        ++edge_count;
      }
  printf(
      "{\"edge_cases\":%u,\"all_output_bytes_and_FPSCR_flags_equal\":true}\n",
      edge_count);
  fflush(stdout);
  // Advanced SIMD uses standard FP arithmetic; preserve arbitrary guest
  // controls and sticky status when the host implementation supplies only new
  // flags.
  unsigned fp_mode_cases = 0;
  for (uint32_t seed : {0u, 0x01000000u, 0x03000000u, 0x00400000u, 0x00800000u,
                        0x00c00000u, 0x8000009fu}) {
    for (bool normal : {false, true}) {
      init(19, 3);
      cpu->set_fpscr(seed);
      if (cpu->fpscr() != seed)
        return 13;
      original(0, 19, normal);
      uint32_t expected_fp = cpu->fpscr();
      std::vector<uint8_t> expected(19 * 12);
      memcpy(expected.data(), p + DEST, expected.size());
      memset(p + DEST, 0xcd, expected.size());
      uint32_t flags = host_skin(p, 0, 19, normal);
      cpu->set_fpscr(seed | (flags & 0x9f));
      if (memcmp(expected.data(), p + DEST, expected.size()) ||
          cpu->fpscr() != expected_fp) {
        fprintf(stderr,
                "Guest FPSCR merge mismatch seed=%08x normal=%d actual=%08x "
                "expected=%08x\n",
                seed, normal, cpu->fpscr(), expected_fp);
        return 14;
      }
      ++fp_mode_cases;
    }
  }
  // The generic profiling bridge also hooks a two-register first PUSH.
  // Compare unchanged ARM entry against exact first-PUSH replay, including
  // stack.
  const uint32_t two_push[] = {0xe92d4080, 0xe1a0700d, 0xe2800001, 0xe8bd8080};
  memcpy(p + 0x12000, two_push, sizeof(two_push));
  cpu->invalidate_cache_range(0x12000, sizeof(two_push));
  auto two_regs = [&](bool replay) {
    auto *r = cpu->regs();
    for (unsigned i = 0; i < 16; ++i)
      r[i] = 0x11223300 + i;
    r[13] = 0x3fff0000;
    r[14] = 0x10000;
    r[15] = 0x12000;
    cpu->set_cpsr(0x10);
    cpu->set_fpscr(0x8000009f);
    if (replay) {
      r[13] -= 8;
      put(r[13], r[7]);
      put(r[13] + 4, r[14]);
      r[15] += 4;
    }
    uint64_t ticks = 1000000;
    if (cpu->run_or_step(&mem, &ticks) != 1)
      exit(15);
  };
  two_regs(false);
  std::array<uint32_t, 16> two_expected;
  memcpy(two_expected.data(), cpu->regs(), sizeof(two_expected));
  uint32_t two_cpsr = cpu->cpsr(), two_fp = cpu->fpscr();
  std::array<uint8_t, 8> two_stack;
  memcpy(two_stack.data(), p + 0x3fff0000 - 8, 8);
  memset(p + 0x3fff0000 - 8, 0xcd, 8);
  two_regs(true);
  if (memcmp(two_expected.data(), cpu->regs(), sizeof(two_expected)) ||
      cpu->cpsr() != two_cpsr || cpu->fpscr() != two_fp ||
      memcmp(two_stack.data(), p + 0x3fff0000 - 8, 8))
    return 16;
  printf("{\"guest_fpscr_modes_equal\":%u,\"two_register_push_replay_equal\":"
         "true}\n",
         fp_mode_cases);
  fflush(stdout);
  constexpr unsigned count = 32768, loops = 32;
  init(count, 3);
  original(0, count, false);
  for (unsigned round = 0; round < 6; ++round) {
    bool host = round == 1 || round == 2 || round == 5;
    auto start = std::chrono::steady_clock::now();
    for (unsigned k = 0; k < loops; ++k) {
      if (host)
        host_skin(p, 0, count, false);
      else
        original(0, count, false);
    }
    auto end = std::chrono::steady_clock::now();
    printf(
        "{\"round\":%u,\"host_native_math\":%s,\"vertices\":%u,\"ms\":%.3f}\n",
        round, host ? "true" : "false", count * loops,
        std::chrono::duration<double, std::milli>(end - start).count());
    fflush(stdout);
  }
  cpu.reset();
  munmap(p, size);
  return 0;
}
