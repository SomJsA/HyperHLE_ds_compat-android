import hashlib
import importlib.util
import io
from pathlib import Path
import tempfile
import unittest
from zipfile import ZipFile

spec = importlib.util.spec_from_file_location('guard', Path(__file__).with_name('guard.py'))
guard = importlib.util.module_from_spec(spec)
spec.loader.exec_module(guard)


class RoleFontArtifactTests(unittest.TestCase):
    def check_package(self, asset_story=None, native_story=True, source_story=None):
        title, story = b'user-title-face', b'user-story-face'
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            fonts = root/'res/overlay'
            fonts.mkdir(parents=True)
            (fonts/'TitleChinese.ttf').write_bytes(title)
            (fonts/'StoryChinese.ttf').write_bytes(story if source_story is None else source_story)
            pinned = {f'res/overlay/{name}.ttf': hashlib.sha256(data).hexdigest()
                      for name, data in [('TitleChinese', title), ('StoryChinese', story)]}
            buffer = io.BytesIO()
            with ZipFile(buffer, 'w') as archive:
                archive.writestr('assets/overlay/TitleChinese.ttf', title)
                archive.writestr('assets/overlay/StoryChinese.ttf', story if asset_story is None else asset_story)
            with ZipFile(io.BytesIO(buffer.getvalue())) as archive:
                return guard.android_role_fonts_in_apk(archive, root, pinned,
                    b'native-library' + title + (story if native_story else b''))

    def test_source_asset_and_native_all_match(self):
        self.assertEqual(self.check_package(), 2)

    def test_stale_java_asset_is_rejected_even_with_correct_native(self):
        with self.assertRaises(guard.GuardError):
            self.check_package(asset_story=b'old-font')

    def test_stale_native_is_rejected_even_with_correct_java_asset(self):
        with self.assertRaises(guard.GuardError):
            self.check_package(native_story=False)

    def test_source_cannot_be_resampled_to_make_the_package_pass(self):
        with self.assertRaises(guard.GuardError):
            self.check_package(source_story=b'changed-input')


if __name__ == '__main__':
    unittest.main()
