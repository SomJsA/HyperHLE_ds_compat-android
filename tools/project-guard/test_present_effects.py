import importlib.util
from pathlib import Path
import unittest

HERE=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('guard',HERE/'guard.py')
guard=importlib.util.module_from_spec(spec);spec.loader.exec_module(guard)
ROOT=HERE.parent.parent

class PresentRoutingRegression(unittest.TestCase):
    def setUp(self):
        self.private=(ROOT/'src/gles/fsr_es2.rs').read_text(encoding='utf-8')
        self.present=(ROOT/'src/gles/present.rs').read_text(encoding='utf-8')
        self.native=(ROOT/'src/gles/gles2_native.rs').read_text(encoding='utf-8')
    def check(self,private=None,present=None,native=None):
        return guard.android_present_effect_contract(private or self.private,present or self.present,native or self.native)
    def test_current_route_and_whitespace_variation(self):
        self.check()
        self.check(private=self.private.replace('present::draw_present_processed_quad(', 'present :: draw_present_processed_quad ('))
    def test_direct_constant_shader_route_cannot_replace_configured_path(self):
        broken=self.private.replace('present::draw_present_processed_quad(', 'present::draw_present_sharpen_quad(')
        with self.assertRaises(guard.GuardError):self.check(private=broken)
    def test_comments_or_continued_strings_cannot_supply_the_required_call(self):
        broken=self.private.replace('present::draw_present_processed_quad(', 'ignored_call(')
        for suffix in ('// present::draw_present_processed_quad();', 'const NOTE: &str = "\\\n present::draw_present_processed_quad()";'):
            with self.subTest(suffix=suffix),self.assertRaises(guard.GuardError):self.check(private=broken+suffix)
    def test_aa_constant_false_cannot_replace_actual_setting_provider(self):
        broken=self.present.replace('let aa = present_aa_mode();','let aa = false;')
        with self.assertRaises(guard.GuardError):self.check(present=broken)
    def test_both_intermediate_targets_require_owned_release(self):
        for target in ('fsr_target','post_target'):
            broken=self.present.replace('self.'+target+'.take()', 'None')
            with self.subTest(target=target),self.assertRaises(guard.GuardError):self.check(present=broken)
    def test_missing_native_uniform_delegate_is_rejected(self):
        broken=self.native.replace('gles2::GetUniformfv(program, location, params)','unimplemented!()')
        with self.assertRaises(guard.GuardError):self.check(native=broken)

if __name__=='__main__':unittest.main()
