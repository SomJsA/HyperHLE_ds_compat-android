/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
//! Allocation-free ordering of spatial present effects. FXAA and sharpening
//! are separate passes: the shader's AA branch samples its input texture and
//! cannot consume the local CAS/RCAS result from the same invocation.

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum Kind { Copy, Easu, Fxaa, Cas, Rcas }

#[derive(Clone, Copy, Debug, PartialEq)]
pub struct Pass { pub kind: Kind, pub sharpen: f32 }

impl Pass {
    const COPY: Self = Self { kind: Kind::Copy, sharpen: 0.0 };
    pub fn fsr(self) -> f32 {
        match self.kind { Kind::Easu => 1.0, Kind::Rcas => 2.0, _ => 0.0 }
    }
    pub fn aa(self) -> f32 { if self.kind == Kind::Fxaa { 1.0 } else { 0.0 } }
}

#[derive(Clone, Copy, Debug, PartialEq)]
pub struct Plan { passes: [Pass; 3], len: usize }

impl Plan {
    pub fn new(fsr_mode: u8, sharpen: f32, aa: bool) -> Self {
        let sharpen = if sharpen.is_finite() { sharpen.clamp(0.0, 2.0) } else { 0.0 };
        let mut result = Self { passes: [Pass::COPY; 3], len: 0 };
        if fsr_mode > 0 {
            result.push(Kind::Easu, 0.0);
            if aa { result.push(Kind::Fxaa, 0.0); }
            // Fast FSR deliberately omits RCAS; quality FSR retains its
            // existing automatic 0.87 shader strength when sharpen is zero.
            if fsr_mode == 1 { result.push(Kind::Rcas, sharpen); }
        } else {
            if aa { result.push(Kind::Fxaa, 0.0); }
            if sharpen > 0.0 { result.push(Kind::Cas, sharpen); }
        }
        if result.len == 0 { result.push(Kind::Copy, 0.0); }
        result
    }
    fn push(&mut self, kind: Kind, sharpen: f32) {
        assert!(self.len < self.passes.len());
        self.passes[self.len] = Pass { kind, sharpen };
        self.len += 1;
    }
    pub fn passes(&self) -> &[Pass] { &self.passes[..self.len] }
    pub fn name(&self) -> &'static str {
        match (self.passes[0].kind, self.len) {
            (Kind::Copy, _) => "copy",
            (Kind::Cas, _) => "cas",
            (Kind::Fxaa, 1) => "fxaa",
            (Kind::Fxaa, _) => "fxaa->cas",
            (Kind::Easu, 1) => "easu",
            (Kind::Easu, 3) => "easu->fxaa->rcas",
            (Kind::Easu, _) if self.passes[1].kind == Kind::Fxaa => "easu->fxaa",
            (Kind::Easu, _) => "easu->rcas",
            _ => unreachable!(),
        }
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct VerifyKey {
    pub program: u32,
    pub source: (u32, u32),
    pub sharpen: u32,
    pub fsr: u32,
    pub aa: u32,
    pub shadow: u32,
}

/// Owned by one PresentState. Disabled mode reserves no entry; diagnostic
/// queries are bounded even if an external controller changes settings.
#[derive(Default)]
pub struct VerifyBudget { seen: [Option<VerifyKey>; 16] }
impl VerifyBudget {
    pub fn admit(&mut self, enabled: bool, key: VerifyKey) -> bool {
        if !enabled || self.seen.contains(&Some(key)) { return false; }
        if let Some(entry) = self.seen.iter_mut().find(|entry| entry.is_none()) {
            *entry = Some(key);
            true
        } else { false }
    }
    pub fn clear(&mut self) { self.seen = [None; 16]; }
}

#[cfg(test)]
mod tests {
    use super::*;
    fn kinds(plan: Plan) -> Vec<Kind> { plan.passes().iter().map(|p| p.kind).collect() }
    #[test]
    fn recommended_aa_and_cas_both_execute_in_order() {
        let p=Plan::new(0,0.5,true);
        assert_eq!(kinds(p),[Kind::Fxaa,Kind::Cas]);
        assert_eq!(p.passes()[0].sharpen,0.0);
        assert_eq!(p.passes()[1].sharpen,0.5);
        assert_eq!(p.name(),"fxaa->cas");
    }
    #[test]
    fn quality_fsr_aa_rcas_never_overwrite_each_other() {
        let p=Plan::new(1,0.5,true);
        assert_eq!(kinds(p),[Kind::Easu,Kind::Fxaa,Kind::Rcas]);
        assert_eq!(p.passes().iter().map(|p| p.fsr()).collect::<Vec<_>>(),[1.0,0.0,2.0]);
    }
    #[test]
    fn fast_fsr_omits_rcas_but_retains_requested_aa() {
        assert_eq!(kinds(Plan::new(2,0.5,true)),[Kind::Easu,Kind::Fxaa]);
        assert_eq!(kinds(Plan::new(2,0.5,false)),[Kind::Easu]);
    }
    #[test]
    fn independent_off_and_single_effect_paths_remain_distinct() {
        assert_eq!(kinds(Plan::new(0,0.0,false)),[Kind::Copy]);
        assert_eq!(kinds(Plan::new(0,0.0,true)),[Kind::Fxaa]);
        assert_eq!(kinds(Plan::new(0,0.5,false)),[Kind::Cas]);
        assert_eq!(kinds(Plan::new(1,0.0,false)),[Kind::Easu,Kind::Rcas]);
    }
    #[test]
    fn no_stage_claims_aa_and_sharpen_together() {
        for fsr in 0..=2 { for amount in [0.0,0.5,2.0] { for aa in [false,true] {
            let p=Plan::new(fsr,amount,aa);
            assert!(p.passes().len()<=3);
            for stage in p.passes() { assert!(!(stage.aa()>0.0 && stage.sharpen>0.0)); }
        }}}
    }
    #[test]
    fn non_finite_sharpen_cannot_reach_uniforms() {
        for value in [f32::NAN,f32::INFINITY,f32::NEG_INFINITY,-1.0] {
            assert_eq!(kinds(Plan::new(0,value,true)),[Kind::Fxaa]);
        }
        assert_eq!(Plan::new(0,3.0,false).passes()[0].sharpen,2.0);
    }
    fn key(program:u32)->VerifyKey { VerifyKey { program,source:(1440,960),sharpen:0.5f32.to_bits(),fsr:0,aa:1f32.to_bits(),shadow:0 } }
    #[test]
    fn verification_is_disabled_and_owned_per_context() {
        let mut a=VerifyBudget::default();let mut b=VerifyBudget::default();
        assert!(!a.admit(false,key(1)));
        assert!(a.admit(true,key(1)));assert!(!a.admit(true,key(1)));
        assert!(b.admit(true,key(1)));
        a.clear();assert!(a.admit(true,key(1)));
    }
    #[test]
    fn verification_budget_is_bounded_without_forgetting_old_entries() {
        let mut b=VerifyBudget::default();
        for program in 1..=16 { assert!(b.admit(true,key(program))); }
        assert!(!b.admit(true,key(17)));assert!(!b.admit(true,key(1)));
    }
}
