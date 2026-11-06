#include <chrono>
#include <memory>
#include <sys/mman.h>
#include <unistd.h>
#include "lib.cpp"

namespace touchHLE::cpu {
struct touchHLE_Mem {unsigned char* p;uint64_t reads=0,writes=0;};
template<class T>T rd(touchHLE_Mem*m,uint32_t a,bool*e){*e=false;++m->reads;if(a<4096)return 0;T v;memcpy(&v,m->p+a,sizeof(v));return v;}
template<class T>bool wr(touchHLE_Mem*m,uint32_t a,T v){++m->writes;if(a>=4096)memcpy(m->p+a,&v,sizeof(v));return false;}
extern "C" {
uint8_t touchHLE_cpu_read_u8(touchHLE_Mem*m,uint32_t a,bool*e){return rd<uint8_t>(m,a,e);}
uint16_t touchHLE_cpu_read_u16(touchHLE_Mem*m,uint32_t a,bool*e){return rd<uint16_t>(m,a,e);}
uint32_t touchHLE_cpu_read_u32(touchHLE_Mem*m,uint32_t a,bool*e){return rd<uint32_t>(m,a,e);}
uint64_t touchHLE_cpu_read_u64(touchHLE_Mem*m,uint32_t a,bool*e){return rd<uint64_t>(m,a,e);}
bool touchHLE_cpu_write_u8(touchHLE_Mem*m,uint32_t a,uint8_t v){return wr(m,a,v);}
bool touchHLE_cpu_write_u16(touchHLE_Mem*m,uint32_t a,uint16_t v){return wr(m,a,v);}
bool touchHLE_cpu_write_u32(touchHLE_Mem*m,uint32_t a,uint32_t v){return wr(m,a,v);}
bool touchHLE_cpu_write_u64(touchHLE_Mem*m,uint32_t a,uint64_t v){return wr(m,a,v);}
}
}
using namespace touchHLE::cpu;
int main(int argc,char**argv){
 if(argc!=2||sysconf(_SC_PAGESIZE)!=4096)return 2;size_t size=size_t(1)<<32;
 auto*p=(unsigned char*)mmap(nullptr,size,PROT_READ|PROT_WRITE,MAP_PRIVATE|MAP_ANONYMOUS,-1,0);if(p==MAP_FAILED)return 3;
 FILE*f=fopen(argv[1],"rb");if(!f)return 4;size_t n=fread(p+0x2000,1,4096,f);fclose(f);if(!n)return 5;
 std::array<uint32_t,16> expected_regs;std::array<uint32_t,1024> expected_mem;uint32_t expected_cpsr=0;bool have=false;
 const bool modes[]={false,true,true,false,false,true};
 for(unsigned round=0;round<6;++round){
  bool fast=modes[round];setenv("TOUCHHLE_DS_CPU_FASTMEM",fast?"1":"0",1);touchHLE_Mem m{p};
  {
   auto owned=std::make_unique<DynarmicWrapper>(p,1); auto& cpu=*owned; // Production wrapper is heap allocated (8MiB page table).
   auto init=[&](uint32_t loops){auto*r=cpu.regs();for(int i=0;i<16;++i)r[i]=0;cpu.set_cpsr(0x10);r[2]=loops;r[4]=1;r[6]=0x10000;r[7]=1024;r[15]=0x2000;for(uint32_t i=0;i<1024;++i)memcpy(p+0x10000+i*4,&i,4);};
   auto run=[&]{uint64_t ticks=100000000000ULL;return cpu.run_or_step(&m,&ticks);};
   init(4);if(run()!=1)return 6;init(100000);m.reads=m.writes=0;auto start=std::chrono::steady_clock::now();if(run()!=1)return 7;auto end=std::chrono::steady_clock::now();
   std::array<uint32_t,16> regs;memcpy(regs.data(),cpu.regs(),sizeof(regs));std::array<uint32_t,1024> mem;memcpy(mem.data(),p+0x10000,sizeof(mem));
   if(!have){expected_regs=regs;expected_mem=mem;expected_cpsr=cpu.cpsr();have=true;}else if(regs!=expected_regs||mem!=expected_mem||cpu.cpsr()!=expected_cpsr)return 8;
   printf("{\"round\":%u,\"production_wrapper\":true,\"fastmem\":%s,\"ms\":%.3f,\"registers_cpsr_memory_equal\":true}\n",round,fast?"true":"false",std::chrono::duration<double,std::milli>(end-start).count());fflush(stdout);
   const uint32_t code[]={0xe5910000,0xe5812000,0xe5913000,0xef000001};memcpy(p+0x3000,code,sizeof(code));cpu.invalidate_cache_range(0x3000,sizeof(code));auto*r=cpu.regs();r[1]=0;r[2]=0xdeadbeef;r[15]=0x3000;m.writes=0;
   if(run()!=1||r[0]!=0||r[3]!=0||m.writes!=1)return 9;
  }
  // The real wrapper destructor must restore protection, with no null writes leaked.
  uint32_t zero;memcpy(&zero,p,4);if(zero!=0)return 10;memset(p,0,4096);
  printf("{\"round\":%u,\"null_fault_and_guard_restore_passed\":true}\n",round);fflush(stdout);
 }
 munmap(p,size);return 0;
}
