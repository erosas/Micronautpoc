package com.bytestream

import io.micronaut.runtime.Micronaut.run


object Application {
	@JvmStatic
	fun main(args: Array<String>) {
		run(Application.javaClass)
	}
}