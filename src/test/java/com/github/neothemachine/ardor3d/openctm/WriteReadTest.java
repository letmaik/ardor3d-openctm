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
 * with JOpenCTM and our wrapper class. 
 * 
 * It does NOT test if JOpenCTM does adhere to the OpenCTM format spec! (see TeapotReferenceTest)
 * 
 * Total correctness is only checked for RAW. For MG1, the correctness
 * of vertices and normals is checked. For MG2, only a simple heuristic is used
 * (volume of bounding box nearly equal).
 * 
 * TODO doesn't check if MG1 triangle reordering works correctly, i.e. it isn't checked if
 *      the same triangles exist after the reordering
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
		float[] uvMap = readUVMap(mesh);
		int[] indices = readIndices(mesh);
		
		// TODO what values can the MG2 UV map precision have?
		AttributeData uvMapData = new AttributeData("", "", 10, uvMap);
		AttributeData[] uvMaps = {uvMapData};
		
		darwin.jopenctm.data.Mesh ctmMesh = new darwin.jopenctm.data.Mesh(
				vertices, normals, indices, uvMaps, new AttributeData[0]);
		
		darwin.jopenctm.data.Mesh loadedMesh = encodeAndDecodeDirect(ctmMesh, enc);
		Mesh loadedMeshArdor = encodeAndDecode(ctmMesh, enc);
		
		check(vertices, normals, indices, uvMap, loadedMesh, enc);
		check(mesh, loadedMeshArdor, enc);
	}
	
	public static float[] readVertices(Mesh mesh) {
		float[] vertices = new float[mesh.getMeshData().getVertexCoords().getTupleCount()*mesh.getMeshData().getVertexCoords().getValuesPerTuple()];
		System.out.println("mesh: " + mesh.getName() + "; verts: " + vertices.length);
		mesh.getMeshData().getVertexBuffer().rewind();
		mesh.getMeshData().getVertexBuffer().get(vertices);
		return vertices;
	}
	
	public static float[] readNormals(Mesh mesh) {
		float[] normals = new float[mesh.getMeshData().getNormalCoords().getTupleCount()*mesh.getMeshData().getVertexCoords().getValuesPerTuple()];
		mesh.getMeshData().getNormalBuffer().rewind();
		mesh.getMeshData().getNormalBuffer().get(normals);
		return normals;
	}
	
	public static int[] readIndices(Mesh mesh) {
		IndexBufferData<?> ind = mesh.getMeshData().getIndices();
		int[] indices = new int[ind.getBufferLimit()];
		ind.asIntBuffer().get(indices);
		return indices;
	}
	
	public static float[] readUVMap(Mesh mesh) {
		float[] uvMap = new float[mesh.getMeshData().getTextureCoords(0).getTupleCount()*mesh.getMeshData().getTextureCoords(0).getValuesPerTuple()];
		mesh.getMeshData().getTextureBuffer(0).rewind();
		mesh.getMeshData().getTextureBuffer(0).get(uvMap);
		return uvMap;
	}
	
	private void check(float[] originalVertices, float[] originalNormals, int[] originalIndices, float[] originalUVMap, darwin.jopenctm.data.Mesh mesh, MeshEncoder enc) {
		assertEquals(originalVertices.length, mesh.vertices.length);
		assertEquals(originalNormals.length, mesh.normals.length);
		assertEquals(originalIndices.length, mesh.indices.length);
		assertEquals(originalUVMap.length, mesh.texcoordinates[0].values.length);
		
		if (!MG2Encoder.class.equals(enc.getClass())) {
			assertArrayEquals(originalVertices, mesh.vertices, Float.MIN_VALUE);
			assertArrayEquals(originalNormals, mesh.normals, Float.MIN_VALUE);
			assertArrayEquals(originalUVMap, mesh.texcoordinates[0].values, Float.MIN_VALUE);
			if (MG1Encoder.class.equals(enc.getClass())) {
				MG1Encoder mg1 = (MG1Encoder) enc;
				mg1.rearrangeTriangles(originalIndices);
			}
			assertArrayEquals(originalIndices, mesh.indices);
		}
	}
	
	private void check(Mesh originalMesh, Mesh newMesh, MeshEncoder enc) {
		float[] originalVertices = readVertices(originalMesh);
		float[] originalNormals = readNormals(originalMesh);
		float[] originalUVMap = readUVMap(originalMesh);
		int[] originalIndices = readIndices(originalMesh);
		
		float[] newVertices = readVertices(newMesh);
		float[] newNormals = readNormals(newMesh);
		float[] newUVMap = readUVMap(newMesh);
		int[] newIndices = readIndices(newMesh);
		
		assertEquals(originalVertices.length, newVertices.length);
		assertEquals(originalNormals.length, newNormals.length);
		assertEquals(originalIndices.length, newIndices.length);
		assertEquals(originalUVMap.length, newUVMap.length);
		
		originalMesh.setModelBound(new BoundingBox());
		newMesh.setModelBound(new BoundingBox());
			
		if (!MG2Encoder.class.equals(enc.getClass())) {
			assertArrayEquals(originalVertices, newVertices, Float.MIN_VALUE);
			assertArrayEquals(originalNormals, newNormals, Float.MIN_VALUE);
			assertArrayEquals(originalUVMap, newUVMap, Float.MIN_VALUE);
			if (MG1Encoder.class.equals(enc.getClass())) {
				MG1Encoder mg1 = (MG1Encoder) enc;
				mg1.rearrangeTriangles(originalIndices);
			}
			assertArrayEquals(originalIndices, newIndices);
		}
		
		if (!RawEncoder.class.equals(enc.getClass())) {
			double accuracy = MG1Encoder.class.equals(enc.getClass()) ? Double.MIN_VALUE : 0.1;

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
