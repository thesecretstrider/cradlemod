Add-Type -AssemblyName System.Drawing

$img = New-Object System.Drawing.Bitmap("C:\Users\aaron\Documents\cradlemod\.claude\image-1.png.png")
Write-Host "Original: $($img.Width)x$($img.Height)"

# Clean transparency
for ($y = 0; $y -lt $img.Height; $y++) {
    for ($x = 0; $x -lt $img.Width; $x++) {
        $p = $img.GetPixel($x, $y)
        if ($p.A -lt 128) {
            $img.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, 0, 0, 0))
        } elseif ($p.A -lt 255) {
            $img.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $p.R, $p.G, $p.B))
        }
    }
}

# Find content bounds
$minX = $img.Width; $minY = $img.Height; $maxX = 0; $maxY = 0
for ($y = 0; $y -lt $img.Height; $y++) {
    for ($x = 0; $x -lt $img.Width; $x++) {
        if ($img.GetPixel($x, $y).A -gt 0) {
            if ($x -lt $minX) { $minX = $x }
            if ($y -lt $minY) { $minY = $y }
            if ($x -gt $maxX) { $maxX = $x }
            if ($y -gt $maxY) { $maxY = $y }
        }
    }
}
$cw = $maxX - $minX + 1
$ch = $maxY - $minY + 1
Write-Host "Content: ${cw}x${ch} at ($minX,$minY)"

# Square crop centered on content
$size = [Math]::Max($cw, $ch)
$centerX = $minX + $cw / 2
$centerY = $minY + $ch / 2
$cropX = [Math]::Max(0, [int]($centerX - $size / 2))
$cropY = [Math]::Max(0, [int]($centerY - $size / 2))
$cropSize = [Math]::Min($size, [Math]::Min($img.Width - $cropX, $img.Height - $cropY))

$cropRect = New-Object System.Drawing.Rectangle($cropX, $cropY, $cropSize, $cropSize)
$cropped = $img.Clone($cropRect, $img.PixelFormat)
$img.Dispose()

# Resize to 16x16
$dest = New-Object System.Drawing.Bitmap(16, 16)
$g = [System.Drawing.Graphics]::FromImage($dest)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$g.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
$g.Clear([System.Drawing.Color]::FromArgb(0, 0, 0, 0))
$g.DrawImage($cropped, 0, 0, 16, 16)
$g.Dispose()
$cropped.Dispose()

# Shift down so content touches bottom row
$maxYFinal = 0
for ($y = 0; $y -lt 16; $y++) {
    for ($x = 0; $x -lt 16; $x++) {
        if ($dest.GetPixel($x, $y).A -gt 0 -and $y -gt $maxYFinal) { $maxYFinal = $y }
    }
}
$gap = 15 - $maxYFinal
Write-Host "Bottom gap: $gap pixels, shifting down"

if ($gap -gt 0) {
    $shifted = New-Object System.Drawing.Bitmap(16, 16)
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $newY = $y + $gap
            if ($newY -lt 16) {
                $shifted.SetPixel($x, $newY, $dest.GetPixel($x, $y))
            }
        }
    }
    $dest.Dispose()
    $shifted.Save("C:\Users\aaron\Documents\cradlemod\src\main\resources\assets\cradlemod\textures\block\vital_fruit_bush.png", [System.Drawing.Imaging.ImageFormat]::Png)
    $shifted.Dispose()
} else {
    $dest.Save("C:\Users\aaron\Documents\cradlemod\src\main\resources\assets\cradlemod\textures\block\vital_fruit_bush.png", [System.Drawing.Imaging.ImageFormat]::Png)
    $dest.Dispose()
}
Write-Host "Done - saved vital_fruit_bush.png"
