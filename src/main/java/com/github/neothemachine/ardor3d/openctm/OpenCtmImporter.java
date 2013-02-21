/*
 * Copyright (C) 2012 Maik Riechert
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * (version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library.  If not, see <http://www.gnu.org/licenses/> 
 * or write to the Free Software Foundation, Inc., 51 Franklin Street,
 * Fifth Floor, Boston, MA 02110-1301  USA.
 */
package com.github.neothemachine.ardor3d.openctm;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.resource.ResourceLocator;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.ResourceSource;

import darwin.jopenctm.io.CtmFileReader;

public class OpenCtmImporter {

	private ResourceLocator modelLocator;

	public OpenCtmImporter setModelLocator(final ResourceLocator locator) {
		this.modelLocator = locator;
		return this;
	}

	/**
	 * Reads an OpenCTM file from the given resource
	 * 
	 * @param resource
	 *            the name of the resource to find.
	 * @return
	 */
	public Mesh load(final String resource) {
		final ResourceSource source;
		if (this.modelLocator == null) {
			source = ResourceLocatorTool.locateResource(
					ResourceLocatorTool.TYPE_MODEL, resource);
		} else {
			source = this.modelLocator.locateResource(resource);
		}

		if (source == null) {
			throw new Error("Unable to locate '" + resource + "'");
		}

		return this.load(source);
	}

	/**
	 * Reads an OpenCTM file from the given resource
	 * 
	 * @param resource
	 *            a resource pointing to the model we wish to load.
	 * @return
	 */
	public Mesh load(final ResourceSource resource) {
        if (resource == null) {
            throw new NullPointerException("Unable to load null resource");
        }
        try{
	    	final CtmFileReader ctmReader = new CtmFileReader(resource.openStream());
	    	darwin.jopenctm.data.Mesh ctmMesh = ctmReader.decode();
	    	
	        final MeshData meshData = new MeshData();
	        
	        IntBuffer indexBuffer = BufferUtils.createIntBuffer(ctmMesh.indices);
	        meshData.setIndexBuffer(indexBuffer);
	        
	        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(ctmMesh.vertices);
	        meshData.setVertexBuffer(vertexBuffer);
	        
	        FloatBuffer normalBuffer = BufferUtils.createFloatBuffer(ctmMesh.normals);
	        meshData.setNormalBuffer(normalBuffer);
	        
	        for (int i = 0; i < ctmMesh.getUVCount(); i++) {
	        	FloatBuffer textureBuffer = BufferUtils.createFloatBuffer(ctmMesh.texcoordinates[i].values);
	        	meshData.setTextureBuffer(textureBuffer, i);
	        }            
	
	        final Mesh mesh = new Mesh();
	        mesh.setMeshData(meshData);
	        mesh.setModelBound(new BoundingBox());
	        mesh.setName(resource.getName());
	
	        return mesh;
        } catch (Exception e) {
        	throw new RuntimeException(e);
        }
            
    }

}
