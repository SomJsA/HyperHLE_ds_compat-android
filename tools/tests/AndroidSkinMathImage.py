"""Read-only original IPA -> standalone math-test memory segments, never an IPA."""
import argparse, hashlib, json, plistlib, struct, zipfile
from pathlib import Path

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--ipa', type=Path, required=True)
p.add_argument('--output', type=Path, required=True)
a = p.parse_args()
assert not a.output.exists(), 'Existing output preserved'
h = hashlib.sha256()
with a.ipa.open('rb') as stream:
    for block in iter(lambda: stream.read(1024 * 1024), b''):
        h.update(block)
assert h.hexdigest() == '1381cbd7de15bcaf8085edf8f191cc54261c00376087d4893ac62fb9b9f2da1c', 'Require accepted immutable original base'
with zipfile.ZipFile(a.ipa) as z:
    names = [n for n in z.namelist() if n.count('/') == 2 and n.endswith('/Info.plist')]
    assert len(names) == 1
    info = plistlib.loads(z.read(names[0]))
    member = names[0].rsplit('/', 1)[0] + '/' + info['CFBundleExecutable']
    binary = z.read(member)
assert struct.unpack_from('<I', binary)[0] == 0xfeedface
ncommands = struct.unpack_from('<I', binary, 16)[0]
offset = 28
segments = []
for _ in range(ncommands):
    command, size = struct.unpack_from('<II', binary, offset)
    assert size >= 8 and offset + size <= len(binary)
    if command == 1:
        name, address, vsize, start, length = struct.unpack_from('<16s4I', binary, offset + 8)
        name = name.rstrip(b'\0').decode('ascii')
        if name in ('__TEXT', '__DATA'):
            assert length <= vsize and start + length <= len(binary)
            segments.append((name, address, binary[start:start + length]))
    offset += size
assert {name for name, _, _ in segments} == {'__TEXT', '__DATA'}
for address, size, expected in [(0x37f814, 692, 55936553603488786), (0x37fac8, 716, 10538268167844965500), (0x53d1a4, 16, 5222870166602214087)]:
    candidates = [(base, data) for _, base, data in segments if base <= address and address + size <= base + len(data)]
    assert len(candidates) == 1
    base, data = candidates[0]
    value = 0xcbf29ce484222325
    for byte in data[address - base:address - base + size]:
        value = ((value ^ byte) * 0x100000001b3) & 0xffffffffffffffff
    assert value == expected, 'Unknown original function'
image = struct.pack('<I', len(segments))
for _, address, data in segments:
    image += struct.pack('<II', address, len(data)) + data
with a.output.open('xb') as stream:
    stream.write(image)
print(json.dumps({'source_ipa_sha256': h.hexdigest(), 'member': member, 'image_bytes': len(image), 'image_sha256': hashlib.sha256(image).hexdigest(), 'derived_IPA': False}))
