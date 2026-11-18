package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.PortableManifestRootV2;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PortableManifestRootV2Test {
    private static final UUID SNAPSHOT=
            UUID.fromString("11111111-2222-4333-8444-555555555555");
    private static Map<String,PortableManifestRootV2.Category> categories() {
        return Map.of(
                "FAMILY_MEMBERS",new PortableManifestRootV2.Category(
                        2,"a".repeat(64),"b".repeat(64)),
                "FAMILY_ROLES",new PortableManifestRootV2.Category(
                        1,"c".repeat(64),"d".repeat(64)));
    }
    @Test void independentBigEndianGoldenVectorAndOrderIndependence() {
        // Independently calculated using Python struct.pack >I/>q and SHA256.
        var expected="d0dc0fd28e2d1defc0a9d13c1092416c0f9c3d166b48438ba17df6b3d0298029";
        assertEquals(expected,PortableManifestRootV2.digest(
                17,SNAPSHOT,7,categories()));
        var reverse=new LinkedHashMap<String,PortableManifestRootV2.Category>();
        reverse.put("FAMILY_ROLES",categories().get("FAMILY_ROLES"));
        reverse.put("FAMILY_MEMBERS",categories().get("FAMILY_MEMBERS"));
        assertEquals(expected,PortableManifestRootV2.digest(17,SNAPSHOT,7,reverse));
    }
    @Test void mutationsAreNeverEquivalentToOneSealedRoot() {
        var original=categories();
        String digest=PortableManifestRootV2.digest(17,SNAPSHOT,7,original);
        assertNotEquals(digest,PortableManifestRootV2.digest(18,SNAPSHOT,7,original));
        assertNotEquals(digest,PortableManifestRootV2.digest(17,UUID.randomUUID(),7,original));
        assertNotEquals(digest,PortableManifestRootV2.digest(17,SNAPSHOT,8,original));
        var mutated=new HashMap<>(original);
        mutated.put("FAMILY_MEMBERS",new PortableManifestRootV2.Category(
                2,"f".repeat(64),"b".repeat(64)));
        assertNotEquals(digest,PortableManifestRootV2.digest(17,SNAPSHOT,7,mutated));
        mutated.put("FAMILY_MEMBERS",new PortableManifestRootV2.Category(
                3,"a".repeat(64),"b".repeat(64)));
        assertNotEquals(digest,PortableManifestRootV2.digest(17,SNAPSHOT,7,mutated));
        mutated.remove("FAMILY_ROLES");
        assertNotEquals(digest,PortableManifestRootV2.digest(17,SNAPSHOT,7,mutated));
    }
    @Test void invalidCategoryAndDigestOrNegativeRowsAreRejected() {
        var invalid=new HashMap<>(categories());
        invalid.put("FAMILY_ROLES",new PortableManifestRootV2.Category(
                -1,"c".repeat(64),"d".repeat(64)));
        assertThrows(SecurityException.class,()->
                PortableManifestRootV2.digest(17,SNAPSHOT,7,invalid));
        invalid.put("FAMILY_ROLES",new PortableManifestRootV2.Category(
                1,"C".repeat(64),"d".repeat(64)));
        assertThrows(SecurityException.class,()->
                PortableManifestRootV2.digest(17,SNAPSHOT,7,invalid));
        invalid.put("unexpected",new PortableManifestRootV2.Category(
                1,"c".repeat(64),"d".repeat(64)));
        assertThrows(SecurityException.class,()->
                PortableManifestRootV2.digest(17,SNAPSHOT,7,invalid));
        assertThrows(SecurityException.class,()->
                PortableManifestRootV2.digest(17,SNAPSHOT,-1,categories()));
    }
}
