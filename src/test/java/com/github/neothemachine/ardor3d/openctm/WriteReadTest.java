package com.github.neothemachine.ardor3d.openctm;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.scenegraph.IndexBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.shape.Capsule;
import com.ardor3d.scenegraph.shape.GeoSphere;
import com.ardor3d.scenegraph.shape.Icosahedron;
import com.ardor3d.scenegraph.shape.Pyramid;
import com.ardor3d.scenegraph.shape.Teapot;
import com.ardor3d.scenegraph.shape.GeoSphere.TextureMode;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.resource.ResourceSource;

import darwin.jopenctm.compression.MG1Encoder;
import darwin.jopenctm.compression.MG2Encoder;
import darwin.jopenctm.compression.MeshEncoder;
import darwin.jopenctm.compression.RawEncoder;
import darwin.jopenctm.data.AttributeData;
import darwin.jopenctm.io.CtmFileReader;
import darwin.jopenctm.io.CtmFileWriter;

/**
 * This tests whether sample Meshes from Ardor3D can be correctly encoded and decoded
 * with JOpenCTM. Correctness is only checked for RAW. For MG1 and MG2, a simple
 * heuristic is used (equal (MG1) / near-equal (MG2) volume of bounding box).
 * 
 * TODO another test should be created which checks against reference MG1/MG2 files
 * 
 * @author maik
 *
 */
public class WriteReadTest {
	
	private static Mesh[] meshes = {
		new Teapot("Teapot"),
		new Capsule("Capsule", 5, 5, 5, 2, 5),
		new Icosahedron("Icosahedron", 3),
		new Pyramid("Pyramid", 2, 4),
		new GeoSphere("GeoSphere", true, 3, 5, TextureMode.Original)
		};

	@Test
	public void testRaw() throws Exception {
		testEncoder(meshes, new RawEncoder());
	}
	
	@Test
	public void testMG1() throws Exception {
		testEncoder(meshes, new MG1Encoder());
	}
	
	@Test
	public void testMG2() throws Exception {
		testEncoder(meshes, new MG2Encoder());
	}
	
	private void testEncoder(Mesh[] meshes, MeshEncoder enc) throws Exception {
		for (Mesh mesh : meshes) {
			writeAndReadMesh(mesh, enc);
		}
	}
	
	private void writeAndReadMesh(Mesh mesh, MeshEncoder enc) throws Exception {
		
		float[] vertices = readVertices(mesh);		
		float[] normals = readNormals(mesh);
		int[] indices = readIndices(mesh);

		AttributeData[] empty = new AttributeData[0];
		darwin.jopenctm.data.Mesh ctmMesh = new darwin.jopenctm.data.Mesh(
				vertices, normals, indices, empty, empty);
		
		check(vertices, normals, indices, ctmMesh, true);

		darwin.jopenctm.data.Mesh loadedMesh = encodeAndDecodeDirect(ctmMesh, enc);
		Mesh loadedMeshArdor = encodeAndDecode(ctmMesh, enc);
		
		boolean checkBuffersEquality = RawEncoder.class.equals(enc.getClass()) ? true : false;
		boolean checkMeshExact = !MG2Encoder.class.equals(enc.getClass()) ? true : false;

		check(vertices, normals, indices, loadedMesh, checkBuffersEquality);
		check(mesh, loadedMeshArdor, checkBuffersEquality, checkMeshExact);
	}
	
	private float[] readVertices(Mesh mesh) {
		float[] vertices = new float[mesh.getMeshData().getVertexCoords().getTupleCount()*mesh.getMeshData().getVertexCoords().getValuesPerTuple()];
		System.out.println("mesh: " + mesh.getName() + "; verts: " + vertices.length);
		mesh.getMeshData().getVertexBuffer().rewind();
		mesh.getMeshData().getVertexBuffer().get(vertices);
		return vertices;
	}
	
	private float[] readNormals(Mesh mesh) {
		float[] normals = new float[mesh.getMeshData().getNormalCoords().getTupleCount()*mesh.getMeshData().getVertexCoords().getValuesPerTuple()];
		mesh.getMeshData().getNormalBuffer().rewind();
		mesh.getMeshData().getNormalBuffer().get(normals);
		return normals;
	}
	
	private int[] readIndices(Mesh mesh) {
		IndexBufferData<?> ind = mesh.getMeshData().getIndices();
		int[] indices = new int[ind.getBufferLimit()];
		ind.asIntBuffer().get(indices);
		return indices;
	}
	
	private void check(float[] originalVertices, float[] originalNormals, int[] originalIndices, darwin.jopenctm.data.Mesh mesh, boolean checkContents) {
		assertEquals(originalVertices.length, mesh.vertices.length);
		assertEquals(originalNormals.length, mesh.normals.length);
		assertEquals(originalIndices.length, mesh.indices.length);	
		
		if (checkContents) {
			assertArrayEquals(originalVertices, mesh.vertices, Float.MIN_VALUE);
			assertArrayEquals(originalNormals, mesh.normals, Float.MIN_VALUE);
			assertArrayEquals(originalIndices, mesh.indices);
		}
	}
	
	private void check(Mesh originalMesh, Mesh newMesh, boolean checkBufferEquality, boolean checkMeshExact) {
		float[] originalVertices = readVertices(originalMesh);		
		float[] originalNormals = readNormals(originalMesh);
		int[] originalIndices = readIndices(originalMesh);
		
		float[] newVertices = readVertices(newMesh);
		float[] newNormals = readNormals(newMesh);
		int[] newIndices = readIndices(newMesh);
		
		assertEquals(originalVertices.length, newVertices.length);
		assertEquals(originalNormals.length, newNormals.length);
		assertEquals(originalIndices.length, newIndices.length);
		
		originalMesh.setModelBound(new BoundingBox());
		newMesh.setModelBound(new BoundingBox());
			
		if (checkBufferEquality) {
			assertArrayEquals(originalVertices, newVertices, Float.MIN_VALUE);
			assertArrayEquals(originalNormals, newNormals, Float.MIN_VALUE);
			assertArrayEquals(originalIndices, newIndices);
		} else {
			double accuracy = checkMeshExact ? Double.MIN_VALUE : 0.1;
			
			// TODO this should include more checks
			assertEquals(originalMesh.getModelBound().getVolume(), 
					newMesh.getModelBound().getVolume(), accuracy);
		}
	}
	
	private darwin.jopenctm.data.Mesh encodeAndDecodeDirect(darwin.jopenctm.data.Mesh mesh, MeshEncoder enc) throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		CtmFileWriter writer = new CtmFileWriter(output, enc);
		writer.encode(mesh, "");
		
		ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
		CtmFileReader reader = new CtmFileReader(input);
		return reader.decode();
	}
	
	private Mesh encodeAndDecode(darwin.jopenctm.data.Mesh mesh, MeshEncoder enc) throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		CtmFileWriter writer = new CtmFileWriter(output, enc);
		writer.encode(mesh, "");
		
		final ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
		OpenCtmImporter importer = new OpenCtmImporter();
		return importer.load(new StreamResource(input));
	}

}

class StreamResource implements ResourceSource {
	
	private InputStream input;
	public StreamResource(InputStream s) {
		this.input = s;
	}
	
	@Override
	public void write(OutputCapsule capsule) throws IOException {
	}
	
	@Override
	public void read(InputCapsule capsule) throws IOException {
	}
	
	@Override
	public Class<?> getClassTag() {
		return null;
	}
	
	@Override
	public InputStream openStream() throws IOException {
		return input;
	}
	@Override
	public String getType() {
		return null;
	}
	@Override
	public ResourceSource getRelativeSource(String name) {
		return null;
	}
	@Override
	public String getName() {
		return "";
	}
}
