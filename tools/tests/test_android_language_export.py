"""The SBIN zero string reference must fall back, never read the table tail."""
import importlib.util
from pathlib import Path
import struct
import tempfile
import unittest
import zipfile

path=Path(__file__).resolve().parents[1]/'translation/Export-AndroidLanguages.py'
spec=importlib.util.spec_from_file_location('export_languages',path)
exporter=importlib.util.module_from_spec(spec);spec.loader.exec_module(exporter)

def bank(value=2):
    strings=[b'Anchor_01',b'Hello',b'UI_DIFFICULTY',b'Select Difficulty',b'YES']
    strs=struct.pack('<I',len(strings))+b''.join(struct.pack('<I',len(s))+s for s in strings)
    data=struct.pack('<IIHHIHHI',16,2,1,value,0,3,value and 4,0)
    out=b'SBIN\x02\0\0\0'
    for tag,payload in [(b'OHDR',struct.pack('<III',0,0,0)),(b'DATA',data),(b'STRS',strs)]:
        h=2166136261
        for b in payload:h=((h*16777619)^b)&0xffffffff
        out+=struct.pack('<4sII',tag,len(payload),h)+payload
        out+=b'\0'*((-len(out))%4)
    return out

class ExportTest(unittest.TestCase):
    def fixture(self,japanese=0,other=0):
        folder=tempfile.TemporaryDirectory();self.addCleanup(folder.cleanup)
        ipa=Path(folder.name)/'source.ipa'
        with zipfile.ZipFile(ipa,'w') as archive:
            for code in ['CHS_CN','ENG_US','FRE_FR','GER_DE','ITA_IT','KOR_KR','SPA_ES','JPN_JP']:
                archive.writestr('Payload/DeadSpace.app/published/stringdata/'+code+'/deadspace_iphone.bin',bank(japanese if code=='JPN_JP' else 2 if code=='ENG_US' else other))
        return ipa
    def test_null_reference_uses_english_and_preserves_input(self):
        ipa=self.fixture();before=ipa.read_bytes();result=exporter.export(ipa)
        self.assertEqual(result['languages']['FRE_FR']['Anchor_01'],'Hello')
        self.assertNotIn('JPN_JP',result['languages'])
        self.assertEqual(result['fallbacks'],{'JPN_JP':'ENG_US'})
        self.assertEqual(ipa.read_bytes(),before)
    def test_unknown_reference_is_rejected(self):
        with self.assertRaises(ValueError):exporter.export(self.fixture(other=999))
    def test_changed_japanese_resource_requires_review(self):
        with self.assertRaises(ValueError):exporter.export(self.fixture(japanese=2))

if __name__=='__main__':unittest.main()
