![Axiom Computing Platform](docs/img/axiom_platform.png)

![GitHub CI Status](https://img.shields.io/github/actions/workflow/status/voxelpi/axiomtoolkit/ci.yml?branch=main&label=CI&style=for-the-badge)
[![Sonatype Nexus (Releases)](https://img.shields.io/nexus/r/net.voxelpi.axiom/axiom-core?server=https%3A%2F%2Frepo.voxelpi.net&nexusVersion=3&style=for-the-badge&label=stable&color=blue)](https://repo.voxelpi.net/#browse/search=keyword%3Daxiom)
[![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/net.voxelpi.axiom/axiom-core?server=https%3A%2F%2Frepo.voxelpi.net&nexusVersion=3&style=for-the-badge&label=dev)](https://repo.voxelpi.net/#browse/search=keyword%3Daxiom)

# Axiom Computing Platform

This repository contains a collection of tools designed specifically for the AXIOM computing platform.
Most relevant are the AXM compiler toolchain for compiling axiom assembly projects,
as well as the bridge module for exchanging data with an AXIOM computer.

Check out the [Documentation](https://axiom.voxelpi.net) for more information!

## Architectures

The following architectures are supported by the tools in this repository:

| Architecture | Supported                      |
|--------------|--------------------------------|
| `ax08`       | ✅                              |
| `ax08-l`     | ✅                              |
| `mcpc08`     | ⚠️ DSP functions not supported |
| `mcpc16`     | ✅                              |

## Modules

The repository contains the following modules:

| Module         | Description                                                                                                                |
|----------------|----------------------------------------------------------------------------------------------------------------------------|
| `axiom-arch`   | Implementation of the supported architectures.                                                                             |
| `axiom-asm`    | Assembler for the axiom-asm language, see the [wiki](https://github.com/VoxelPi/Axiom/wiki/Assembly) for more information. |
| `axiom-bridge` | A tool to communicate with the computers bridge, providing feature like uploading programs.                                |
| `axiom-core`   | The core library for all axiom tools.                                                                                      |
| `axiom-cli`    | The cli application.                                                                                                       |
