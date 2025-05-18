package net.voxelpi.axiom

public object AxiomBuildParameters {

    /**
     * The current version.
     */
    public const val VERSION: String = "{{ version }}"

    /**
     * The current git commit.
     */
    public const val GIT_COMMIT: String = "{{ git_commit }}"

    /**
     * The current git commit.
     */
    public const val GIT_BRANCH: String = "{{ git_branch }}"
}
