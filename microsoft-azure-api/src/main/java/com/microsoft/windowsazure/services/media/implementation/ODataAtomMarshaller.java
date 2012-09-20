/**
 * Copyright 2012 Microsoft Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.microsoft.windowsazure.services.media.implementation;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;

import com.microsoft.windowsazure.services.media.implementation.atom.ContentType;
import com.microsoft.windowsazure.services.media.implementation.atom.EntryType;
import com.microsoft.windowsazure.services.media.implementation.atom.FeedType;
import com.microsoft.windowsazure.services.media.implementation.content.AssetType;
import com.microsoft.windowsazure.services.media.implementation.content.Constants;

/**
 * A class to manage marshalling of request parameters into
 * ATOM entry elements for sending to the Media Services REST
 * endpoints.
 * 
 */
public class ODataAtomMarshaller {
    private final Marshaller marshaller;
    private final DocumentBuilder documentBuilder;

    public ODataAtomMarshaller() throws JAXBException, ParserConfigurationException {
        JAXBContext context = JAXBContext.newInstance(getMarshalledClasses(), null);
        marshaller = context.createMarshaller();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        documentBuilder = dbf.newDocumentBuilder();
    }

    public Document marshalEntry(Object content) throws JAXBException {
        ContentType atomContent = new ContentType();
        atomContent.setType("application/xml");
        atomContent.getContent().add(
                new JAXBElement(new QName(Constants.ODATA_METADATA_NS, "properties"), content.getClass(), content));

        EntryType atomEntry = new EntryType();
        atomEntry.getEntryChildren().add(
                new JAXBElement(new QName(Constants.ATOM_NS, "content"), ContentType.class, atomContent));

        JAXBElement<EntryType> entryElement = new JAXBElement<EntryType>(new QName(Constants.ATOM_NS, "entry"),
                EntryType.class, atomEntry);

        Document doc = documentBuilder.newDocument();
        doc.setXmlStandalone(true);

        marshaller.marshal(entryElement, doc);

        return doc;

    }

    private static Class<?>[] getMarshalledClasses() {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        classes.add(FeedType.class);
        classes.add(EntryType.class);
        classes.add(AssetType.class);
        return classes.toArray(new Class<?>[0]);
    }
}
