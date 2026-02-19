Add-Type -AssemblyName System.Drawing

function Fix-Texture($path) {
    $img = New-Object System.Drawing.Bitmap($path)

    # Find lowest non-transparent row
    $maxY = 0
    for ($y = 0; $y -lt $img.Height; $y++) {
        for ($x = 0; $x -lt $img.Width; $x++) {
            $p = $img.GetPixel($x, $y)
            if ($p.A -gt 0 -and $y -gt $maxY) { $maxY = $y }
        }
    }

    $gap = ($img.Height - 1) - $maxY
    Write-Host "$path - bottom gap: $gap pixels"

    if ($gap -le 0) {
        Write-Host "  Already at bottom, skipping"
        $img.Dispose()
        return
    }

    # Create new image with content shifted down by $gap pixels
    $w = $img.Width
    $h = $img.Height
    $dest = New-Object System.Drawing.Bitmap($w, $h)

    for ($y = 0; $y -lt $h; $y++) {
        for ($x = 0; $x -lt $w; $x++) {
            $newY = $y + $gap
            if ($newY -lt $h) {
                $dest.SetPixel($x, $newY, $img.GetPixel($x, $y))
            }
        }
    }

    $img.Dispose()
    $dest.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $dest.Dispose()
    Write-Host "  Shifted down by $gap pixels"
}

Fix-Texture "C:\Users\aaron\Documents\cradlemod\src\main\resources\assets\cradlemod\textures\block\vital_fruit_bush.png"
Fix-Texture "C:\Users\aaron\Documents\cradlemod\src\main\resources\assets\cradlemod\textures\block\spirit_fruit_bush.png"
