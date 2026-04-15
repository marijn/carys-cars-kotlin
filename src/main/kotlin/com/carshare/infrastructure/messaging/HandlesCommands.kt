package com.carshare.infrastructure.messaging

/**
 * Point of entry to hide implementation details of how commands get handled.
 *
 * Examples of possible handlers could be
 *
 *  - Legacy implementations
 *  - Deciders
 *  - Aggregates
 *  - External Systems
 */
interface HandlesCommands {
    fun handle(command: Command)
}
