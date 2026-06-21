#!/usr/bin/env python3
import os
import random
import struct
import zlib


ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def png_chunk(kind, data):
    body = kind + data
    return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)


def write_png(path, width, height, pixels):
    rows = []
    for y in range(height):
        row = bytearray([0])
        for x in range(width):
            row.extend(pixels[y][x])
        rows.append(bytes(row))
    data = b"".join(rows)
    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    payload = b"\x89PNG\r\n\x1a\n"
    payload += png_chunk(b"IHDR", ihdr)
    payload += png_chunk(b"IDAT", zlib.compress(data, 9))
    payload += png_chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as handle:
        handle.write(payload)


def canvas(width, height, color=(0, 0, 0, 0)):
    return [[bytearray(color) for _ in range(width)] for _ in range(height)]


def set_px(img, x, y, color):
    if 0 <= y < len(img) and 0 <= x < len(img[0]):
        img[y][x] = bytearray(color)


def rect(img, x0, y0, x1, y1, color):
    for y in range(y0, y1):
        for x in range(x0, x1):
            set_px(img, x, y, color)


def outline(img, x0, y0, x1, y1, color):
    rect(img, x0, y0, x1, y0 + 1, color)
    rect(img, x0, y1 - 1, x1, y1, color)
    rect(img, x0, y0, x0 + 1, y1, color)
    rect(img, x1 - 1, y0, x1, y1, color)


def line(img, x0, y0, x1, y1, color):
    dx = abs(x1 - x0)
    dy = -abs(y1 - y0)
    sx = 1 if x0 < x1 else -1
    sy = 1 if y0 < y1 else -1
    err = dx + dy
    x, y = x0, y0
    while True:
        set_px(img, x, y, color)
        if x == x1 and y == y1:
            break
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x += sx
        if e2 <= dx:
            err += dx
            y += sy


def noisy_tile(base, dark, light, seed):
    rng = random.Random(seed)
    img = canvas(16, 16, base)
    for y in range(16):
        for x in range(16):
            roll = rng.random()
            if roll < 0.18:
                set_px(img, x, y, dark)
            elif roll > 0.86:
                set_px(img, x, y, light)
    return img


def wood_texture():
    img = noisy_tile((132, 86, 40, 255), (91, 55, 28, 255), (178, 124, 62, 255), 12)
    for y in (3, 8, 13):
        line(img, 0, y, 15, y + (1 if y == 8 else 0), (82, 48, 24, 255))
    for x in (5, 11):
        line(img, x, 0, x - 1, 15, (162, 108, 54, 255))
    return img


def dark_wood_texture():
    img = noisy_tile((78, 49, 30, 255), (48, 30, 20, 255), (112, 73, 43, 255), 18)
    for x in (2, 8, 14):
        line(img, x, 0, x, 15, (42, 27, 18, 255))
    return img


def trim_texture():
    img = noisy_tile((197, 148, 55, 255), (132, 92, 34, 255), (235, 195, 96, 255), 22)
    outline(img, 0, 0, 16, 16, (105, 73, 28, 255))
    rect(img, 4, 4, 12, 6, (240, 210, 112, 255))
    rect(img, 6, 9, 10, 11, (117, 78, 30, 255))
    return img


def light_texture():
    img = noisy_tile((227, 207, 157, 255), (185, 156, 103, 255), (248, 234, 190, 255), 31)
    for y in (2, 7, 12):
        line(img, 1, y, 14, y, (199, 174, 121, 255))
    return img


def paper_texture():
    img = noisy_tile((229, 214, 175, 255), (183, 157, 108, 255), (247, 237, 206, 255), 44)
    outline(img, 1, 1, 15, 15, (154, 119, 72, 255))
    line(img, 4, 5, 12, 4, (116, 83, 49, 255))
    line(img, 3, 9, 10, 11, (116, 83, 49, 255))
    line(img, 7, 3, 6, 13, (184, 72, 46, 255))
    return img


def sample_item():
    img = canvas(16, 16)
    rect(img, 3, 2, 13, 15, (229, 214, 175, 255))
    outline(img, 3, 2, 13, 15, (122, 90, 55, 255))
    rect(img, 5, 4, 11, 5, (196, 154, 83, 255))
    line(img, 5, 8, 10, 6, (94, 71, 46, 255))
    line(img, 5, 10, 11, 12, (94, 71, 46, 255))
    rect(img, 8, 8, 10, 10, (204, 72, 45, 255))
    rect(img, 11, 13, 13, 15, (170, 139, 92, 255))
    return img


def sample_item_completed():
    img = canvas(16, 16)
    rect(img, 3, 2, 13, 15, (229, 214, 175, 255))
    outline(img, 3, 2, 13, 15, (92, 111, 62, 255))
    rect(img, 5, 4, 11, 5, (182, 169, 91, 255))
    line(img, 5, 8, 8, 7, (73, 103, 57, 255))
    line(img, 8, 7, 11, 9, (73, 103, 57, 255))
    line(img, 5, 11, 8, 10, (73, 103, 57, 255))
    line(img, 8, 10, 11, 12, (73, 103, 57, 255))
    rect(img, 8, 8, 10, 10, (80, 173, 85, 255))
    rect(img, 11, 13, 13, 15, (112, 153, 85, 255))
    set_px(img, 12, 12, (231, 222, 154, 255))
    return img


def gui_texture():
    img = canvas(248, 224, (0, 0, 0, 0))
    rect(img, 0, 0, 248, 224, (112, 78, 45, 255))
    rect(img, 3, 3, 245, 221, (151, 105, 56, 255))
    rect(img, 7, 7, 241, 135, (217, 197, 151, 255))
    rect(img, 38, 18, 240, 126, (233, 218, 181, 255))
    outline(img, 38, 18, 240, 126, (117, 78, 41, 255))
    for y in range(23, 122, 13):
        line(img, 42, y, 235, y - 2, (215, 192, 145, 255))
    for x in range(48, 236, 23):
        line(img, x, 20, x - 10, 124, (222, 202, 158, 255))
    rect(img, 9, 112, 35, 138, (82, 56, 34, 255))
    rect(img, 11, 114, 33, 136, (219, 197, 151, 255))
    outline(img, 12, 115, 31, 134, (65, 42, 25, 255))
    rect(img, 44, 142, 206, 196, (92, 62, 38, 255))
    rect(img, 44, 200, 206, 218, (92, 62, 38, 255))
    for row in range(3):
        for col in range(9):
            sx = 44 + col * 18
            sy = 142 + row * 18
            rect(img, sx, sy, sx + 18, sy + 18, (67, 45, 28, 255))
            rect(img, sx + 1, sy + 1, sx + 17, sy + 17, (207, 184, 139, 255))
            rect(img, sx + 2, sy + 2, sx + 16, sy + 16, (236, 218, 173, 255))
    for col in range(9):
        sx = 44 + col * 18
        sy = 200
        rect(img, sx, sy, sx + 18, sy + 18, (67, 45, 28, 255))
        rect(img, sx + 1, sy + 1, sx + 17, sy + 17, (207, 184, 139, 255))
        rect(img, sx + 2, sy + 2, sx + 16, sy + 16, (236, 218, 173, 255))
    rect(img, 9, 28, 35, 98, (91, 60, 35, 255))
    rect(img, 13, 32, 31, 94, (151, 105, 56, 255))
    outline(img, 14, 33, 30, 93, (65, 42, 25, 255))
    return img


def logo():
    img = canvas(64, 64, (0, 0, 0, 0))
    rect(img, 8, 10, 56, 50, (126, 82, 43, 255))
    rect(img, 12, 8, 52, 18, (226, 205, 154, 255))
    rect(img, 16, 13, 48, 39, (232, 218, 181, 255))
    outline(img, 16, 13, 48, 39, (87, 59, 36, 255))
    for x0, y0, x1, y1 in [(22, 22, 34, 19), (34, 19, 43, 28), (22, 22, 30, 32), (30, 32, 43, 28)]:
        line(img, x0, y0, x1, y1, (177, 126, 45, 255))
    for x, y, c in [(22, 22, (216, 72, 48, 255)), (34, 19, (91, 137, 204, 255)), (30, 32, (86, 167, 91, 255)), (43, 28, (222, 191, 78, 255))]:
        rect(img, x - 3, y - 3, x + 4, y + 4, c)
        outline(img, x - 3, y - 3, x + 4, y + 4, (61, 42, 27, 255))
    return img


def main():
    write_png(os.path.join(ROOT, "src/main/resources/assets/recipe_linkage/textures/block/research_table_wood.png"), 16, 16, wood_texture())
    write_png(os.path.join(ROOT, "src/main/resources/assets/recipe_linkage/textures/block/research_table_dark.png"), 16, 16, dark_wood_texture())
    write_png(os.path.join(ROOT, "src/main/resources/assets/recipe_linkage/textures/block/research_table_trim.png"), 16, 16, trim_texture())
    write_png(os.path.join(ROOT, "src/main/resources/assets/recipe_linkage/textures/block/research_table_light.png"), 16, 16, light_texture())
    write_png(os.path.join(ROOT, "src/main/resources/assets/recipe_linkage/textures/block/research_paper.png"), 16, 16, paper_texture())
    write_png(os.path.join(ROOT, "src/main/resources/assets/recipe_linkage/textures/item/research_sample.png"), 16, 16, sample_item())
    write_png(os.path.join(ROOT, "src/main/resources/assets/recipe_linkage/textures/item/research_sample_completed.png"), 16, 16, sample_item_completed())
    write_png(os.path.join(ROOT, "src/main/resources/assets/recipe_linkage/textures/gui/research_table.png"), 248, 224, gui_texture())
    write_png(os.path.join(ROOT, "src/main/resources/logo.png"), 64, 64, logo())


if __name__ == "__main__":
    main()
