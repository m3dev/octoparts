# Parts Vagrantbox

__The Vagrant VM and tools in this directory are OSX specific__

## Prerequisites

The following are assumed to be installed on your system.

1. [Virtualbox](https://www.virtualbox.org)
2. [Vagrant](http://www.vagrantup.com/)
3. [Ansible](http://www.ansible.com/get-started)

## Running the VM

Starting from the `vaygrant` directory:

1. `$ vagrant up` to bring up the box and provision (this will take a while)
2. `$ vagrant ssh` to get into the VM

__Note__ Some steps of the provisioning may error-out because of timeouts.
In that case, simply run `vagrant provision` to retry. An example of such an error
is `Error: failure: repodata/...-filelists.sqlite.bz2 from epel: [Errno 256] No more mirrors to try.`

## Where is the project in the VM?

`$ cd /parts`

## Forwarded ports

As seen in`./Vagrantfile` ports 9000, 8080, 8443, 5432, 5080 and 5005 are forwarded to your host machine.

## Hystrix-dashboard

Hystrix-dashboard is deployed on this VM on Tomcat via a war file (see `hystrix-dashboard` folder)
and can be accessed on your host machine at port 8080 (also at the same port in the VM).

If you provisioned your VM before hystrix-dashboard was added to this project, simply run
`$ vagrant provision` to provision Tomcat and deploy this war to your
VM at port 8081.

To access the dashboard, open [localhost:8080](http://localhost:8080/dashboard) and enter `http://localhost:9000/hystrix.stream`
as the stream address, then hit "Monitor Stream" (make sure you're running Octoparts on 9000 and this
is forwarded to your host at 9000 as well).

## SBT

[SBT](http://www.scala-sbt.org/) is provisioned automatically and you can use `$ sbt-debug 5005`
to start a debuggable session (you can use another port if you wish, but be sure to change
your Vagrantfile to forward your custom port)

If you put a `.sbtconfig` file in your home folder, you can use it to set options that will
be passed to `sbt-debug` and `sbt` as well.
