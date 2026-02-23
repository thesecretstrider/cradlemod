Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem

# Extract vanilla wolf
$jar = "C:\Users\aaron\.gradle\caches\fabric-loom\1.21.11\minecraft-client.jar"
$zip = [System.IO.Compression.ZipFile]::OpenRead($jar)
$entry = $zip.GetEntry("assets/minecraft/textures/entity/wolf/wolf.png")
$stream = $entry.Open()
$vanillaOut = "C:\Users\aaron\Documents\cradlemod\.claude\vanilla_wolf.png"
$file = [System.IO.File]::Create($vanillaOut)
$stream.CopyTo($file)
$file.Close()
$stream.Close()
$zip.Dispose()

$vanilla = [System.Drawing.Bitmap]::new($vanillaOut)
$custom = [System.Drawing.Bitmap]::new("C:\Users\aaron\Documents\cradlemod\.claude\New Piskel.png")

Write-Host "Vanilla: $($vanilla.Width)x$($vanilla.Height)"
Write-Host "Custom:  $($custom.Width)x$($custom.Height)"
Write-Host ""

# Print a visual ASCII map of both textures (every 2 pixels)
# '#' = opaque pixel, '.' = transparent
Write-Host "=== VANILLA (64x32) ==="
for ($y = 0; $y -lt $vanilla.Height; $y++) {
    $line = ""
    for ($x = 0; $x -lt $vanilla.Width; $x++) {
        $p = $vanilla.GetPixel($x, $y)
        if ($p.A -gt 0) { $line += "#" } else { $line += "." }
    }
    Write-Host ("{0,2}: {1}" -f $y, $line)
}

Write-Host ""
Write-Host "=== CUSTOM PISKEL (64x64) ==="
for ($y = 0; $y -lt $custom.Height; $y++) {
    $line = ""
    for ($x = 0; $x -lt $custom.Width; $x++) {
        $p = $custom.GetPixel($x, $y)
        if ($p.A -gt 0) { $line += "#" } else { $line += "." }
    }
    Write-Host ("{0,2}: {1}" -f $y, $line)
}

$vanilla.Dispose()
$custom.Dispose()
