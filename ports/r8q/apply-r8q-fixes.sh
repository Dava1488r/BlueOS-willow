#!/bin/bash
# Galaxy S20 FE Snapdragon (r8q) on LineageOS 24.0: ports from the 24.0 tablets on the same sm8250-common what r8q lacks.
# See README.md next to this file. Run from the top of the tree after `repo sync`. Idempotent; commits locally in
# device/samsung/r8q and vendor/samsung/r8q (a `repo sync` drops these commits: run the script again).
set -e
cd "${ANDROID_BUILD_TOP:-$PWD}"
D=device/samsung/r8q
V=vendor/samsung/r8q
[ -d $D ] && [ -d $V ] || { echo "run from the top of the tree, with $D and $V synced"; exit 1; }
U=https://raw.githubusercontent.com/TheMuppets/proprietary_vendor_samsung_sm8250-common/lineage-23.2/proprietary/vendor/lib

# recording stack from the common blobs of lineage-23.2 (24.0 made it a dummy there)
mkdir -p $V/proprietary/vendor/lib
for f in "lib_SamsungRec_07020.so 345b025f21c201c8613a8829fb4d45ffd99236efebd06ed55f736314bb889f7f" \
         "librecordalive.so a866a592cae92db65f6461d0e37c7d7b942879fe00e8af56f2c15f1d13caffcb"; do
  set -- $f
  [ "$(sha256sum "$V/proprietary/vendor/lib/$1" 2>/dev/null | cut -d' ' -f1)" = "$2" ] ||
    curl -sfL --retry 8 --retry-delay 10 --max-time 300 "$U/$1" -o "$V/proprietary/vendor/lib/$1"
  [ "$(sha256sum "$V/proprietary/vendor/lib/$1" | cut -d' ' -f1)" = "$2" ] || { echo "wrong checksum: $1"; exit 1; }
done

python3 - <<'EOF'
D, V = 'device/samsung/r8q/', 'vendor/samsung/r8q/'

# HiFi speaker routes: PRI_TDM_RX_0 is r8q's own speaker backend (see its deep-buffer-playback speaker)
p = D + 'configs/audio/mixer_paths.xml'
s = open(p).read()
if 'hifi-playback speaker"' not in s:
    mark = '    <path name="hifi-playback display-port">'
    assert s.count(mark) == 1
    assert '<path name="deep-buffer-playback speaker">\n        <ctl name="PRI_TDM_RX_0 Audio Mixer MultiMedia1"' in s
    add = ('    <path name="hifi-playback speaker">\n'
           '        <ctl name="PRI_TDM_RX_0 Audio Mixer MultiMedia2" value="1" />\n'
           '    </path>\n\n'
           '    <path name="hifi-playback speaker-protected">\n'
           '        <path name="hifi-playback speaker" />\n'
           '    </path>\n\n')
    open(p, 'w').write(s.replace(mark, add + mark))
    print('mixer_paths: HiFi speaker routes added')

p = D + 'proprietary-files.txt'
s = open(p).read()
if 'lib_SamsungRec_07020' not in s:
    mark = '# Audio (amplifier firmware)\n'
    assert s.count(mark) == 1
    open(p, 'w').write(s.replace(mark, '# Audio recording\nvendor/lib/lib_SamsungRec_07020.so\nvendor/lib/librecordalive.so\n\n' + mark))
    print('proprietary-files: recording stack added')

def module(name, libs):
    deps = ''.join(f'                "{l}",\n' for l in libs)
    return ('cc_prebuilt_library_shared {\n'
            f'    name: "{name}",\n    owner: "samsung",\n    strip: {{\n        none: true,\n    }},\n'
            '    target: {\n        android_arm: {\n            srcs: [\n'
            f'                "proprietary/vendor/lib/{name}.so",\n            ],\n            shared_libs: [\n{deps}            ],\n'
            '        },\n    },\n    compile_multilib: "32",\n    prefer: true,\n    soc_specific: true,\n}\n\n')

p = V + 'Android.bp'
s = open(p).read()
if 'lib_SamsungRec_07020' not in s:
    mark = 'cc_prebuilt_library_shared {'
    i = s.index(mark)
    add = module('lib_SamsungRec_07020', ['libc', 'libm', 'libdl', 'liblog']) + \
          module('librecordalive', ['liblog', 'libutils', 'libcutils', 'lib_SamsungRec_07020', 'libsecaudioinfo', 'libc++', 'libc', 'libm', 'libdl'])
    open(p, 'w').write(s[:i] + add + s[i:])
    print('Android.bp: modules added')

p = V + 'r8q-vendor.mk'
s = open(p).read()
if 'lib_SamsungRec_07020' not in s:
    mark = 'PRODUCT_PACKAGES += \\\n'
    assert mark in s
    open(p, 'w').write(s.replace(mark, mark + '    lib_SamsungRec_07020 \\\n    librecordalive \\\n', 1))
    print('r8q-vendor.mk: packages added')
EOF

if [ -n "$(git -C $D status --porcelain)" ]; then
  git -C $D add -A
  git -C $D commit -q -m 'r8q: BlueOS: port HiFi speaker routes and the recording stack from the 24.0 tablets'
fi
if [ -n "$(git -C $V status --porcelain)" ]; then
  git -C $V add -A
  git -C $V commit -q -m 'r8q: BlueOS: device-specific audio recording stack from common lineage-23.2'
fi
git -C $D log --oneline -n 1
git -C $V log --oneline -n 1
