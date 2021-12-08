package me.yung.frechet.bb;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.field.curve.CurveElement;
import it.unisa.dia.gas.plaf.jpbc.field.gt.GTFiniteElement;
import org.apache.commons.compress.utils.IOUtils;

import java.io.IOException;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ElemSerde {

    public static void serializeElem(ZipOutputStream zos, String entryName, Element element) throws IOException {
        if (element instanceof CurveElement) {
            entryName += ".g1";
        } else if (element instanceof GTFiniteElement) {
            entryName += ".gt";
        }
        zos.putNextEntry(new ZipEntry(entryName));
        zos.write(element.toBytes());
    }

    public static Element deserializeElem(ZipInputStream zis, Pairing pairing, String expectedEntryName) throws IOException {
        ZipEntry entry = zis.getNextEntry();
        assert Objects.requireNonNull(entry).getName().startsWith(expectedEntryName);
        byte[] bytes = IOUtils.toByteArray(zis);
        Element elem = null;
        if (entry.getName().endsWith(".g1"))
            elem = pairing.getG1().newElementFromBytes(bytes);
        else if (entry.getName().endsWith(".gt"))
            elem = pairing.getGT().newElementFromBytes(bytes);
        return Objects.requireNonNull(elem).getImmutable();
    }
}
