Add-Type -AssemblyName System.Drawing

$src = "C:\Users\aaron\Documents\cradlemod\.claude\New Piskel.png"
$dstDir = "C:\Users\aaron\Documents\cradlemod\src\main\resources\assets\minecraft\textures\entity\wolf"

# Custom is 64x64 with artwork starting at row 15, col 9
# Vanilla is 64x32 with artwork starting at row 0, col 0
# Need to crop: source rect (9, 15, 64, 32) -> dest rect (0, 0, 64, 32)
# But content goes to col 9+55=64, so width from col 9 is 55 pixels
# Actually the vanilla content doesn't fill all 64 cols either - let me just
# offset the source by (-9, -15) and draw onto a 64x32 canvas

$original = [System.Drawing.Bitmap]::new($src)
Write-Host "Original: $($original.Width)x$($original.Height)"

$result = [System.Drawing.Bitmap]::new(64, 32)
$g = [System.Drawing.Graphics]::FromImage($result)
$g.Clear([System.Drawing.Color]::Transparent)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half

# Draw the original shifted left by 9 and up by 15
# Destination point: (-9, -15) means the image is drawn offset
$g.DrawImage($original, -9, -15)
$g.Dispose()
$original.Dispose()

Write-Host "Result: $($result.Width)x$($result.Height)"

# Verify content position
$firstCol = 64
$firstRow = 32
for ($y = 0; $y -lt $result.Height; $y++) {
    for ($x = 0; $x -lt $result.Width; $x++) {
        if ($result.GetPixel($x, $y).A -gt 0) {
            if ($x -lt $firstCol) { $firstCol = $x }
            if ($y -lt $firstRow) { $firstRow = $y }
        }
    }
}
Write-Host "First content at: col $firstCol, row $firstRow (should be ~col 0-4, row 0)"

# Save to all 27 variant files
$names = @(
    "wolf", "wolf_tame", "wolf_angry",
    "wolf_ashen", "wolf_ashen_tame", "wolf_ashen_angry",
    "wolf_black", "wolf_black_tame", "wolf_black_angry",
    "wolf_chestnut", "wolf_chestnut_tame", "wolf_chestnut_angry",
    "wolf_rusty", "wolf_rusty_tame", "wolf_rusty_angry",
    "wolf_snowy", "wolf_snowy_tame", "wolf_snowy_angry",
    "wolf_spotted", "wolf_spotted_tame", "wolf_spotted_angry",
    "wolf_striped", "wolf_striped_tame", "wolf_striped_angry",
    "wolf_woods", "wolf_woods_tame", "wolf_woods_angry"
)

foreach ($name in $names) {
    $dest = Join-Path $dstDir "$name.png"
    $result.Save($dest, [System.Drawing.Imaging.ImageFormat]::Png)
}

$result.Dispose()
Write-Host "Done! All $($names.Count) files saved"
