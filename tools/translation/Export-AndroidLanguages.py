#!/usr/bin/env python3
"""Export original string banks for Android Canvas; never writes the IPA."""
import argparse
import hashlib
import importlib.util
import json
import struct
import zipfile
from pathlib import Path

def export(ipa):
    spec=importlib.util.spec_from_file_location('ledger',Path(__file__).with_name('Update-Ledger.py'))
    ledger=importlib.util.module_from_spec(spec);spec.loader.exec_module(ledger)
    languages={};sources={}
    with zipfile.ZipFile(ipa) as archive:
        for name in archive.namelist():
            if '/published/stringdata/' not in name or not name.endswith('/deadspace_iphone.bin'):continue
            raw=archive.read(name);_,sections=ledger.sections(raw)
            strings=ledger.unpack_strings(sections[b'STRS']);data=sections[b'DATA']
            offset=struct.unpack_from('<I',sections[b'OHDR'],8)[0]>>3
            kind,count=struct.unpack_from('<II',data,offset)
            if kind!=16 or offset+8+count*8!=len(data):raise ValueError('Unexpected string records')
            values={}
            for index in range(count):
                key,value,_=struct.unpack_from('<HHI',data,offset+8+index*8)
                if not 1<=key<=len(strings) or not 0<=value<=len(strings):raise ValueError('String reference out of bounds')
                if strings[key-1] in values:raise ValueError('Duplicate string key')
                # SBIN string id zero is null, not the final string in STRS.
                values[strings[key-1]]=strings[value-1] if value else ''
            language=name.split('/')[-2];languages[language]=values
            sources[language]={'member':name,'sha256':hashlib.sha256(raw).hexdigest(),'records':count}
    expected={'CHS_CN','ENG_US','FRE_FR','GER_DE','ITA_IT','KOR_KR','SPA_ES','JPN_JP'}
    if set(languages)!=expected:raise ValueError('Unexpected bundled language set')
    english=languages['ENG_US']
    for code,values in languages.items():
        if code=='JPN_JP':continue
        if set(values)!=set(english):raise ValueError('Incomplete original language: '+code)
        if any('\ufffd' in value for value in values.values()):raise ValueError('Invalid original Unicode')
    if set(languages['JPN_JP'].values())!={''}:raise ValueError('Japanese empty-resource assumption changed; review required')
    # Preserve the original metadata, but use English for absent original text.
    for code,values in languages.items():
        if code not in ('CHS_CN','JPN_JP'):
            languages[code]={key:value or english.get(key,'') for key,value in values.items()}
    return {'schema':1,'sources':sources,'fallbacks':{'JPN_JP':'ENG_US'},
            'languages':{code:values for code,values in languages.items() if code not in ('CHS_CN','JPN_JP')}}

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--ipa',required=True,type=Path);parser.add_argument('--output',required=True,type=Path)
    args=parser.parse_args();result=export(args.ipa)
    args.output.parent.mkdir(parents=True,exist_ok=True)
    args.output.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({'languages':len(result['languages']),'records_each':996,'japanese':'explicit English fallback','output':str(args.output)}))
