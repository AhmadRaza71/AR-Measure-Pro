package com.example.data

enum class MeasurementType(val displayName: String, val iconName: String) {
    DISTANCE("Distance", "straighten"),
    CHAIN("Chain Path", "polyline"),
    ANGLE("Angle", "square_foot"),
    CIRCLE_DIAMETER("Circle & Diameter", "circle"),
    CIRCLE_RECESS("Recess & Depth", "vertical_align_bottom"),
    AREA("Polygon Area", "crop_free"),
    VOLUME("3D Cuboid Volume", "view_in_ar"),
    SPIRIT_LEVEL("Surface Incline", "screen_rotation")
}
