![Axiom Computing Platform](docs/img/axiom_platform.png)

![GitHub CI Status](https://img.shields.io/github/actions/workflow/status/voxelpi/axiom/ci.yml?branch=main&label=CI&style=for-the-badge)
[![Sonatype Nexus (Releases)](https://img.shields.io/nexus/r/net.voxelpi.axiom/axiom-core?server=https%3A%2F%2Frepo.voxelpi.net&nexusVersion=3&style=for-the-badge&label=stable&color=blue)](https://repo.voxelpi.net/#browse/search=keyword%3Daxiom)
[![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/net.voxelpi.axiom/axiom-core?server=https%3A%2F%2Frepo.voxelpi.net&nexusVersion=3&style=for-the-badge&label=dev)](https://repo.voxelpi.net/#browse/search=keyword%3Daxiom)

# Axiom Computing Platform

Axiom is a custom computing platform. 
This repository contains a collection of programs designed to make development for that platform easier.

## Architectures

The following architectures are supported by the tools in this repository:

| Architecture | Supported                      |
|--------------|--------------------------------|
| `ax08`       | ✅                              |
| `dev16`      | ✅                              |
| `mcpc08`     | ⚠️ DSP functions not supported |
| `mcpc16`     | ✅                              |

## Modules

The repository contains the following modules:

| Module       | Description                                    |
|--------------|------------------------------------------------|
| `axiom-arch` | Implementation of the supported architectures. |
| `axiom-asm`  | Assembler for the axiom-asm language.          |
| `axiom-core` | The core library for all axiom tools.          |
| `axiom-cli`  | The cli application.                           |
