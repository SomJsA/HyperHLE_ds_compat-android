// Android arm64 pure resource math. No Environment, GL or guest-memory access.
#include <cstdint>
#include <cstring>
#if defined(__aarch64__)
#include <arm_neon.h>
extern "C" uint32_t touchHLE_skin_math(float *dst, const float *pos,
                                       const float *wgt, const int8_t *idx,
                                       const float *mat, uint32_t count,
                                       uint32_t normal) {
  uint64_t fpcr, fpsr;
  asm volatile("mrs %0, fpcr" : "=r"(fpcr));
  asm volatile("mrs %0, fpsr" : "=r"(fpsr));
  uint64_t clear = 0;
  asm volatile("msr fpsr, %0" ::"r"(clear));
  uint64_t standard = (fpcr & ~((3ULL << 22) | 0x9f00ULL)) | (3ULL << 24);
  asm volatile("msr fpcr, %0" ::"r"(standard));
  for (unsigned i = 0; i < count; ++i) {
    float32x4_t result = vdupq_n_f32(0);
    for (unsigned b = 0; b < 4; ++b) {
      float w = wgt[4 * i + b];
      uint32_t bits;
      memcpy(&bits, &w, 4);
      if (b && !(bits & 0x70000000))
        break;
      const float *m = mat + unsigned(idx[4 * i + b]) * 16;
      auto v = vmulq_n_f32(vld1q_f32(m), pos[3 * i]);
      v = vaddq_f32(v, vmulq_n_f32(vld1q_f32(m + 4), pos[3 * i + 1]));
      v = vaddq_f32(v, vmulq_n_f32(vld1q_f32(m + 8), pos[3 * i + 2]));
      if (!normal)
        v = vaddq_f32(v, vld1q_f32(m + 12));
      auto weighted = vmulq_n_f32(v, w);
      result = b ? vaddq_f32(result, weighted) : weighted;
    }
    float out[4];
    vst1q_f32(out, result);
    memcpy(dst + 3 * i, out, 12);
  }
  uint64_t flags;
  asm volatile("mrs %0, fpsr" : "=r"(flags));
  asm volatile("msr fpcr, %0" ::"r"(fpcr));
  asm volatile("msr fpsr, %0" ::"r"(fpsr));
  return uint32_t(flags) & 0x9f;
}
#else
extern "C" uint32_t touchHLE_skin_math(float *, const float *, const float *,
                                       const int8_t *, const float *, uint32_t,
                                       uint32_t) {
  return 0;
}
#endif
