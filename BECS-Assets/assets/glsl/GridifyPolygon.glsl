#version 430

layout(std430, binding = 0) buffer PolygonBuffer {
    vec2 polyVert[];  // Polygon vertices
};

layout(std430, binding = 1) buffer GridBuffer {
    ivec2 gridCells[]; // Grid cell centers
};

layout(std430, binding = 2) buffer OutputBuffer {
    uvec2 validHashes[]; // Valid hashes for the cells
};

uniform int cellSize;

// Function prototypes
bool pointInPoly(vec2 point, int n);
bool lineIntersectsRect(vec2 p1, vec2 p2, vec2 min, vec2 max);
bool segmentsIntersect(vec2 a1, vec2 a2, vec2 b1, vec2 b2);
int orientation(vec2 p, vec2 q, vec2 r);
bool onSegment(vec2 p, vec2 q, vec2 r);
uvec2 computeHash(vec2 cellCenter);

// Mod function
float mod(float a, float b) {
    return a - (b * floor(a / b));
}

// Point in polygon test
bool pointInPoly(vec2 point, int n) {
    bool inside = false;
    for (int i = 0; i < n; i++) {
        vec2 vi = polyVert[i];
        vec2 vj = polyVert[(i + 1) % n];
        
        if ((vi.y > point.y) != (vj.y > point.y) &&
            (point.x < (vj.x - vi.x) * (point.y - vi.y) / (vj.y - vi.y) + vi.x)) {
            inside = !inside;
        }
    }
    return inside;
}

// Hash computation
uvec2 computeHash(vec2 cellCenter) {
    int x = int(floor(cellCenter.x / float(cellSize)));
    int y = int(floor(cellCenter.y / float(cellSize)));
    
    return uvec2(x, y); // Return as unsigned integer vector
}

// Line intersects rectangle test
bool lineIntersectsRect(vec2 p1, vec2 p2, vec2 min, vec2 max) {
    vec2 rectPoints[4];
    rectPoints[0] = vec2(min.x, min.y);
    rectPoints[1] = vec2(max.x, min.y);
    rectPoints[2] = vec2(max.x, max.y);
    rectPoints[3] = vec2(min.x, max.y);

    for (int i = 0; i < 4; i++) {
        vec2 rectStart = rectPoints[i];
        vec2 rectEnd = rectPoints[(i + 1) % 4]; // Next point wraps around

        if (segmentsIntersect(p1, p2, rectStart, rectEnd)) {
            return true; // Found an intersection
        }
    }
    return false; // No intersection found
}

// Segment intersection check
bool segmentsIntersect(vec2 a1, vec2 a2, vec2 b1, vec2 b2) {
    int o1 = orientation(a1, a2, b1);
    int o2 = orientation(a1, a2, b2);
    int o3 = orientation(b1, b2, a1);
    int o4 = orientation(b1, b2, a2);

    // General case
    if (o1 != o2 && o3 != o4) {
        return true;
    }

    // Special cases
    if (o1 == 0 && onSegment(a1, b1, a2)) return true;
    if (o2 == 0 && onSegment(a1, b2, a2)) return true;
    if (o3 == 0 && onSegment(b1, a1, b2)) return true;
    if (o4 == 0 && onSegment(b1, a2, b2)) return true;

    return false; // No intersection
}

// Orientation check
int orientation(vec2 p, vec2 q, vec2 r) {
    float val = (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y);
    if (val == 0.0) return 0; // Collinear
    return (val > 0.0) ? 1 : 2; // Clockwise or counterclockwise
}

// Check if point is on segment
bool onSegment(vec2 p, vec2 q, vec2 r) {
    return (q.x <= max(p.x, r.x) && q.x >= min(p.x, r.x) &&
            q.y <= max(p.y, r.y) && q.y >= min(p.y, r.y));
}

void main() {
    uint id = gl_GlobalInvocationID.x;
    ivec2 cellCenter = gridCells[id];
    
    vec2 cellMin = vec2(cellCenter) - vec2(cellSize, cellSize);
    vec2 cellMax = vec2(cellCenter) + vec2(cellSize, cellSize);
    
    bool intersects = false;
    int n = polyVert.length();
    
    for (int i = 0; i < n; i++) {
        vec2 cV = polyVert[i];
        vec2 nV = polyVert[int(mod(i + 1, n))];  // Casting mod result to int
        
        if (lineIntersectsRect(cV, nV, cellMin, cellMax)) {
            intersects = true;
            break;
        }
    }
    
    bool contains = pointInPoly(vec2(cellCenter), n);
    if (intersects || contains) {
        validHashes[id] = computeHash(vec2(cellCenter)); // Store hash in output buffer
    }
}
