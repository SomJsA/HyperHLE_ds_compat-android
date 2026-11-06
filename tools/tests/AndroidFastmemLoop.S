.syntax unified
.arm
.text
.global _start
_start:
outer:
 mov r0,r6
 mov r1,r7
inner:
 ldr r5,[r0],#4
 add r3,r3,r5
 eor r5,r5,r4
 add r4,r4,#1
 str r5,[r0,#-4]
 subs r1,r1,#1
 bne inner
 subs r2,r2,#1
 bne outer
 svc #1
