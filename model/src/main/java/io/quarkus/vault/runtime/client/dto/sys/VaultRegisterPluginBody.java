package io.quarkus.vault.runtime.client.dto.sys;

import java.util.List;

public class VaultRegisterPluginBody {

    public String version;

    public String sha256;

    public String command;

    public List<String> args;

    public List<String> env;

}
