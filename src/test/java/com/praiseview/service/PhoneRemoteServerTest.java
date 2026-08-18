package com.praiseview.service;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;

class PhoneRemoteServerTest {

    @Test
    void exposesTheDocumentedRemoteDefaults() {
        assertEquals(8080, PhoneRemoteServer.DEFAULT_PORT);
        assertEquals("/praiseview", PhoneRemoteServer.PATH);
    }

    @Test
    void localAddressLookupReturnsAUsableAddress() {
        String address = PhoneRemoteServer.findLocalIpAddress();

        assertNotNull(address);
        assertFalse(address.isBlank());
        assertDoesNotThrow(() -> InetAddress.getByName(address));
    }
}
