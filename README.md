# Powertask Slack

Powertask Slack is a library for building powerful workflows with Slack interactions. 

* Automate your processes and integrate them with the tool you already use!
* Build slack bots with complex behaviour

Powertask is built on top of the absolutely incredible [Camunda BPM](https://camunda.com) stack.

## Features

### Tasks
* Tasks announced on Slack
* Task forms rendered in a message, for simple tasks like approvals
* Complex tasks rendered as modals in Slack
* Dynamic Task Chaining: multiple subsequent tasks, based on workflow decisions rendered in the same modal, for seamless user experience
* Displaying a task description for a task
* Displaying a configurable set of process variables for a task 

### Form elements
* String, Boolean, Long, Date and Enumeration field types
* Length and value validation
* Slack hints, placeholders 

### Spring support
* Spring Boot starters

### Upcoming
* Listing tasks on the Home screen
* Starting new processes from the Home screen
* Slack events as process start events
* Slash commands as process start events
* Support for remaining Slack form elements
* Custom Field validators
* CDI support
* Slack as authentication mechanism for the Camunda Web Applications

## Documentation

See the docs directory for:

* [Reference Documentation](docs/reference.md)
* [Getting Started Guide](docs/getting-started.md)
* [The Powertask Cookbook](docs/cookbook.md)

## Contributing

Powertask Slack is avaiable under the Apache License v2.0. We welcome contributions.
