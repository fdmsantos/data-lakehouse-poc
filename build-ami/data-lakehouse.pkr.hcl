build {

  sources = [ # TODO
    "amazon-ebs.web-server"
  ]

  // Updating Instance
  provisioner "shell" {

    inline = [
      "sudo apt-get update -y"
    ]
  }

  // Install Hadoop
  provisioner "shell" {
    inline = [
      "sudo apt-get -y install openjdk-8-jdk-headless",
      "wget http://apache.cs.utah.edu/hadoop/common/hadoop-3.3.4/hadoop-3.3.4.tar.gz",
      "tar -xzf hadoop-3.3.4.tar.gz",
      "sudo mv hadoop-3.3.4 /usr/local/hadoop"
      # TODO The Following Lines
      Configure Hadoop ENvironment variables and hadoop.env (use readlink?? => Search)
      Configure Pseudo Distribution (Set Setup passphraseless ssh) => https://hadoop.apache.org/docs/stable/hadoop-project-dist/hadoop-common/SingleCluster.html#Download
      Start and Stop should go to user data??
    ]
  }

  // Install Docker
  provisioner "shell" {
    inline = [
      # TODO The Following Lines
      Install Docker (https://www.digitalocean.com/community/tutorials/how-to-install-and-use-docker-on-ubuntu-20-04)
        If necessary install docker compose plugin ( sudo apt-get install docker-compose-plugin)
    ]
  }




}