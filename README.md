OpenCTM importer for Ardor3D
============================

This project uses Daniel Heinrich's [JOpenCTM](https://github.com/Danny02/JOpenCTM) library to read
[OpenCTM](http://openctm.sourceforge.net/) files and use them as meshes in [Ardor3D](http://ardor3d.com).

It is basically just a single wrapper class. The hard work was done by Daniel Heinrich who ported
the OpenCTM reader from C to Java.

Why OpenCTM?
============

Well, it's damn fast! On my machine it takes ~2-3s to load an 8 MiB Wavefront (.obj) file and ~130ms
to load the same mesh as OpenCTM file (3.3 MiB in RAW mode).

How to use
==========

1. Checkout this project and install it in your local Maven repo
2. Load models as usual:

    Mesh mesh = new OpenCtmImporter().load("...");
    root.attachChild(mesh);
