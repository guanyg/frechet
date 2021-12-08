package me.yung.frechet.bb;

import org.jlinalg.Matrix;

import java.io.*;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@SuppressWarnings({"rawtypes", "unchecked"})
public class TokenKey {
    private Matrix m1;
    private Matrix m2;
    private BigInteger[] gamma1;
    private BigInteger[] gamma2;

    public static TokenKey load(InputStream is, IPSPE ipspe) {
        if (!(is instanceof BufferedInputStream))
            is = new BufferedInputStream(is);
        TokenKey tk = new TokenKey();
        try (ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry = zis.getNextEntry();
            assert Objects.requireNonNull(entry).getName().equals("m1");
            tk.m1 = new Matrix((Matrix) new ObjectInputStream(zis).readObject(), ipspe.fpFactory);

            entry = zis.getNextEntry();
            assert entry.getName().equals("m2");
            tk.m2 = new Matrix((Matrix) new ObjectInputStream(zis).readObject(), ipspe.fpFactory);

            entry = zis.getNextEntry();
            assert entry.getName().equals("gamma1");
            tk.gamma1 = (BigInteger[]) new ObjectInputStream(zis).readObject();

            entry = zis.getNextEntry();
            assert entry.getName().equals("gamma2");
            tk.gamma2 = (BigInteger[]) new ObjectInputStream(zis).readObject();

            return tk;
        } catch (IOException | ClassNotFoundException ioException) {
            ioException.printStackTrace();
        }
        return null;
    }

    public Matrix getM1() {
        return m1;
    }

    public void setM1(Matrix m1) {
        this.m1 = m1;
    }

    public Matrix getM2() {
        return m2;
    }

    public void setM2(Matrix m2) {
        this.m2 = m2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TokenKey tk = (TokenKey) o;
        return Objects.equals(m1, tk.m1) &&
                Objects.equals(m2, tk.m2) &&
                Arrays.equals(gamma1, tk.gamma1) &&
                Arrays.equals(gamma2, tk.gamma2);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(m1, m2);
        result = 31 * result + Arrays.hashCode(gamma1);
        result = 31 * result + Arrays.hashCode(gamma2);
        return result;
    }

    public BigInteger[] getGamma1() {
        return gamma1;
    }

    public void setGamma1(BigInteger[] gamma1) {
        this.gamma1 = gamma1;
    }

    public BigInteger[] getGamma2() {
        return gamma2;
    }

    public void setGamma2(BigInteger[] gamma2) {
        this.gamma2 = gamma2;
    }

    public void serialize(OutputStream os) {
        if (!(os instanceof BufferedOutputStream))
            os = new BufferedOutputStream(os);

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(os)) {
            zipOutputStream.putNextEntry(new ZipEntry("m1"));
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(zipOutputStream);
            objectOutputStream.writeObject(m1);
            objectOutputStream.flush();
            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("m2"));
            objectOutputStream = new ObjectOutputStream(zipOutputStream);
            objectOutputStream.writeObject(m2);
            objectOutputStream.flush();
            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("gamma1"));
            objectOutputStream = new ObjectOutputStream(zipOutputStream);
            objectOutputStream.writeObject(gamma1);
            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("gamma2"));
            objectOutputStream = new ObjectOutputStream(zipOutputStream);
            objectOutputStream.writeObject(gamma2);
            zipOutputStream.closeEntry();

        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

}