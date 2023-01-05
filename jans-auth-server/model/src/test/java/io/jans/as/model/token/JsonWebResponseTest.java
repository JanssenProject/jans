package io.jans.as.model.token;

import io.jans.as.model.BaseTest;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwt.JwtType;
import io.jans.as.model.util.Pair;
import org.testng.annotations.Test;

import java.io.*;
import java.util.Calendar;
import java.util.Date;

import static io.jans.as.model.jwt.JwtClaimName.EXPIRATION_TIME;
import static org.testng.Assert.*;

public class JsonWebResponseTest extends BaseTest {

    @Test
    public void serialization_object_correctSerialization() throws IOException, ClassNotFoundException {
        showTitle("serialization_object_correctSerialization");

        JsonWebResponse toWrite = new JsonWebResponse();
        toWrite.getHeader().setAlgorithm(SignatureAlgorithm.RS256);
        toWrite.getHeader().setType(JwtType.JWT);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MILLISECOND, 0);
        toWrite.getClaims().setExpirationTime(cal.getTime());
        toWrite.getClaims().setClaim("testClaim", 45L);

        FileOutputStream fileOut = new FileOutputStream("fileJsonWebResponse.ser");
        ObjectOutputStream out = new ObjectOutputStream(fileOut);
        out.writeObject(toWrite);
        out.close();
        fileOut.close();

        JsonWebResponse deserealized = null;
        FileInputStream fileIn = new FileInputStream("fileJsonWebResponse.ser");
        ObjectInputStream in = new ObjectInputStream(fileIn);
        deserealized = (JsonWebResponse) in.readObject();
        in.close();
        fileIn.close();

        assertEquals(toWrite.getHeader().getSignatureAlgorithm(), deserealized.getHeader().getSignatureAlgorithm());
        assertEquals(toWrite.getHeader().getType(), deserealized.getHeader().getType());
        assertEquals(toWrite.getClaims().getClaimAsDate(EXPIRATION_TIME), deserealized.getClaims().getClaimAsDate(EXPIRATION_TIME));
        assertEquals(toWrite.getClaims().getClaimAsLong("testClaim"), deserealized.getClaims().getClaimAsLong("testClaim"));
    }

}
