package com.sorodeveloper.insaatcebimde.domain.model

data class DeletedNodeDetail(
    val node: ProjectNode,
    val fullPath: String,
    val deletedChildrenNames: List<String>
)
