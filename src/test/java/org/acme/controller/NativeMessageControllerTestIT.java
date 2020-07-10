package org.acme.controller;

import org.acme.controller.MessageControllerTest;

import io.quarkus.test.junit.NativeImageTest;

@NativeImageTest
public class NativeMessageControllerTestIT extends MessageControllerTest {

    // Execute the same tests but in native mode.
}