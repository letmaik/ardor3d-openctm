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

As JOpenCTM isn't published to a Maven repository yet, you have to check-out JOpenCTM manually 
and do a Maven install.

Then check-out this project and load models as usual:

    Mesh mesh = new OpenCtmImporter().load("...");
    root.attachChild(mesh);
    
Known issues
============

As my goal is absolutely fast loading I only use RAW OpenCTM files which are bigger but don't have to
be decompressed while importing. A quick test trying to load an MG2 model lead to exceptions (while
unzipping) and faulty data. So for now, your best bet is to only use RAW in conjunction with JOpenCTM
until the bugs are fixed.