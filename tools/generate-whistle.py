"""Generate the original 32 px GUI sprite and the world model material atlas."""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/witchersbestiary/textures/item"
icon = Image.new("RGBA", (32, 32))
draw = ImageDraw.Draw(icon)
draw.polygon([(2,20),(7,15),(11,15),(11,12),(18,5),(25,4),(29,8),(29,15),(22,22),(16,24),(12,23),(7,28),(3,27),(1,24)], fill="#282537")
draw.polygon([(3,21),(8,16),(13,17),(15,21),(7,26),(4,25)], fill="#8993a6")
draw.line([(3,21),(8,17),(12,17)], fill="#f6f0dc", width=2)
draw.line([(5,24),(10,20)], fill="#646878", width=2)
draw.polygon([(12,13),(19,6),(25,5),(28,8),(28,14),(21,21),(17,23),(13,21),(11,17)], fill="#929eb2")
draw.polygon([(12,13),(19,6),(25,5),(28,8),(21,15),(15,17)], fill="#e1dccd")
draw.line([(13,13),(19,7),(24,6)], fill="#fff7de", width=2)
draw.polygon([(15,16),(21,14),(27,9),(27,14),(21,20),(17,21)], fill="#666b80")
draw.polygon([(15,12),(19,8),(23,8),(23,11),(19,15),(16,15)], fill="#343043")
draw.line([(16,12),(19,9),(22,9)], fill="#626478")
draw.polygon([(18,17),(22,13),(25,14),(25,17),(21,21),(18,20)], fill="#383148")
draw.polygon([(19,17),(22,14),(24,15),(24,17),(21,20),(19,19)], fill="#a37cc6")
draw.line([(20,17),(22,15),(23,15)], fill="#eec9fc")
draw.point((23,18), fill="#674585")
draw.line([(11,17),(13,19),(13,21)], fill="#d2ccba")
draw.line([(25,6),(27,8),(26,10)], fill="#f6f0dc")
draw.line([(17,22),(21,21),(23,19)], fill="#a0a8ad")
icon.save(ROOT / "unicorn_whistle.png")

atlas = Image.new("RGBA", (32, 32), "#777989")
draw = ImageDraw.Draw(atlas)
for x, y, palette in (
    (0, 0, ("#fff5dd", "#d6d3c4", "#a5aab1", "#69758a")),
    (16, 0, ("#ebedf1", "#bec5d1", "#818da1", "#474e61")),
    (0, 16, ("#49435a", "#332d43", "#252333", "#383348")),
    (16, 16, ("#efd4ff", "#b884de", "#9367bb", "#604382")),
):
    for row in range(16):
        draw.line((x,y+row,x+15,y+row), fill=palette[min(row//4,3)])
    draw.line((x,y,x,y+15), fill=palette[1])
    draw.line((x+15,y,x+15,y+15), fill=palette[3])
atlas.save(ROOT / "unicorn_whistle_world.png")
