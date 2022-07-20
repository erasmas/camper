{ pkgs ? import <nixpkgs> {} }:

pkgs.mkShell {
  buildInputs = with pkgs; [
    clojure-lsp
    geckodriver
    jdk
    leiningen
  ];
}
