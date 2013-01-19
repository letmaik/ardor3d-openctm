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

import java.nio.*;

import darwin.jopenctm.io.CtmFileReader;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.scenegraph.*;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.resource.ResourceSource;

public class OpenCtmImporter {

    /**
     * Reads an OpenCTM file from the given resource
     *
     * @param resource a resource pointing to the model we wish to load.
     * @return
     */
    public Mesh load(ResourceSource resource) {
        if (resource == null) {
            throw new NullPointerException("Unable to load null resource");
        }
        try {
            CtmFileReader ctmReader = new CtmFileReader(resource.openStream());
            darwin.jopenctm.data.Mesh ctmMesh = ctmReader.decode();
            
            //checks if the mesh data is usable, but can make loading time longer(?)
            //ctmMesh.checkIntegrity();

            MeshData meshData = new MeshData();
            meshData.setIndexBuffer(IntBuffer.wrap(ctmMesh.indices));
            meshData.setVertexBuffer(FloatBuffer.wrap(ctmMesh.vertices));
            if (ctmMesh.hasNormals()) {
                meshData.setNormalBuffer(FloatBuffer.wrap(ctmMesh.normals));
            }

            for (int i = 0; i < ctmMesh.getUVCount(); i++) {
                meshData.setTextureBuffer(FloatBuffer.wrap(ctmMesh.texcoordinates[i].values), i);
            }
            
            //other attributes like tangent, fog or color could be added through parsing of generic attributs

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
