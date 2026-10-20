# Writes a gzipped structure NBT: an X x Y x Z box (default 5x3x5) with a stone floor at y=0 and air above.
# Usage: python tools/make_test_structure.py <out.nbt> [X Y Z]
# Used for the GameTest platforms in src/main/resources/data/minecraftmod/structures (see docs/testing.md).
import gzip, struct, sys

def s(v): b = v.encode(); return struct.pack('>H', len(b)) + b
def named(tag, name, payload): return bytes([tag]) + s(name) + payload
def i(v): return struct.pack('>i', v)
def compound(items): return b''.join(items) + b'\x00'
def lst(tag, elems): return bytes([tag]) + i(len(elems)) + b''.join(elems)
def int_list(vals): return lst(3, [i(v) for v in vals])

X, Y, Z = (int(v) for v in sys.argv[2:5]) if len(sys.argv) > 4 else (5, 3, 5)
palette = [compound([named(8, 'Name', s('minecraft:stone'))]),
           compound([named(8, 'Name', s('minecraft:air'))])]
blocks = []
for x in range(X):
    for y in range(Y):
        for z in range(Z):
            blocks.append(compound([named(9, 'pos', int_list([x, y, z])), named(3, 'state', i(0 if y == 0 else 1))]))

root = named(10, '', compound([
    named(3, 'DataVersion', i(3465)),  # Minecraft 1.20.1
    named(9, 'size', int_list([X, Y, Z])),
    named(9, 'palette', lst(10, palette)),
    named(9, 'blocks', lst(10, blocks)),
    named(9, 'entities', lst(10, [])),
]))
with gzip.open(sys.argv[1], 'wb') as f:
    f.write(root)
