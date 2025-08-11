#!/usr/bin/bash
# ./make_icons.sh

mkdir -p mipmap-hdpi
mkdir -p mipmap-mdpi
mkdir -p mipmap-xhdpi
mkdir -p mipmap-xxhdpi
mkdir -p mipmap-xxxhdpi

# Create PNG icons in mipmap folders.
for spec in "mipmap-hdpi:48" "mipmap-mdpi:72" "mipmap-xhdpi:96" "mipmap-xxhdpi:144" "mipmap-xxxhdpi:192"; do
  dir="${spec%%:*}"
  size="${spec##*:}"
  for name in ic_launcher ic_launcher_round; do
    cp "$size.png" "$dir/$name.png"
  done
done
