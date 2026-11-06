//! Deferred error belongs to a GL context, not its temporary borrowed dispatch handle.
use std::cell::Cell;
#[derive(Default)]
pub(super) struct Pending(Cell<u32>);
impl Pending {
    pub(super) fn save(&self, error:u32) {
        if error!=0 && self.0.get()==0 {self.0.set(error);}
    }
    pub(super) fn take(&self)->u32 {self.0.replace(0)}
}
#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn error_survives_dispatch_borrow_and_is_consumed_once() {
        let context=Pending::default();
        {let dispatch=&context;dispatch.save(0x500);}
        {let next_dispatch=&context;assert_eq!(next_dispatch.take(),0x500);}
        assert_eq!(context.take(),0);
    }
    #[test]
    fn unrelated_contexts_and_success_cannot_erase_the_first_error() {
        let first=Pending::default();let second=Pending::default();
        first.save(0x500);first.save(0);first.save(0x502);second.save(0x501);
        assert_eq!(first.take(),0x500);assert_eq!(second.take(),0x501);
        first.save(0x502);assert_eq!(first.take(),0x502);assert_eq!(second.take(),0);
    }
}
