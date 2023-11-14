package de.fhg.ipa.fhg.aas_transformer.service.test;

public class AasTemplateRendererTestConfig {

    public static final String SUBMODEL_ANSIBLE_FACTS = """
                {
              "idShort":"ansible-facts",
              "identification":{
                "idType":"Custom",
                "id":"ansible-facts"
              },
              "dataSpecification": [],
              "embeddedDataSpecifications": [],
              "modelType":{
                "name":"Submodel"
              },
              "kind":"Instance",
              "semanticId":{
                "keys":[
                  {
                    "type":"ConceptDescription",
                    "idType":"IRI",
                    "value":"https://docs.ansible.com/ansible/latest/playbook_guide/playbooks_vars_facts.html#ansible-facts",
                    "local":false
                  }
                ]
              },
              "parent": {
                "keys": [
                  {
                    "type": "AssetAdministrationShell",
                    "local": true,
                    "value": "aas-id",
                    "idType": "Custom"
                  }
                ]
              },
              "submodelElements":[
                {
                  "idShort":"distribution",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"Ubuntu"
                },
                {
                  "idShort":"distribution_release",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"jammy"
                },
                {
                  "idShort":"distribution_version",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"22.04"
                },
                {
                  "idShort":"distribution_major_version",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"22"
                },
                {
                  "idShort":"distribution_file_path",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"/etc/os-release"
                },
                {
                  "idShort":"distribution_file_variety",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"Debian"
                },
                {
                  "idShort":"os_family",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"Debian"
                },
                {
                  "idShort":"user_id",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"ansible"
                },
                {
                  "idShort":"user_gecos",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"ansible"
                },
                {
                  "idShort":"user_dir",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"/home/ansible"
                },
                {
                  "idShort":"user_shell",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"/bin/bash"
                },
                {
                  "idShort":"virtualization_role",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"guest"
                },
                {
                  "idShort":"virtualization_type",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"VMware"
                },
                {
                  "idShort":"system",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"Linux"
                },
                {
                  "idShort":"kernel",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"5.15.0-60-generic"
                },
                {
                  "idShort":"kernel_version",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"#66-Ubuntu SMP Fri Jan 20 14:29:49 UTC 2023"
                },
                {
                  "idShort":"machine",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"x86_64"
                },
                {
                  "idShort":"python_version",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"3.10.6"
                },
                {
                  "idShort":"fqdn",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"ubuntu2204.main.ansible.fortknox.local"
                },
                {
                  "idShort":"hostname",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"ubuntu2204"
                },
                {
                  "idShort":"nodename",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"ubuntu2204"
                },
                {
                  "idShort":"domain",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"main.ansible.fortknox.local"
                },
                {
                  "idShort":"userspace_bits",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"64"
                },
                {
                  "idShort":"architecture",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"x86_64"
                },
                {
                  "idShort":"userspace_architecture",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"x86_64"
                },
                {
                  "idShort":"machine_id",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"6a7fb7ebae3e486da9afc371f8058731"
                },
                {
                  "idShort":"system_capabilities_enforced",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"True"
                },
                {
                  "idShort":"ssh_host_key_dsa_public",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"AAAAB3NzaC1kc3MAAACBAPySMJuxKdSxCWU9tHzzWzhk5o3TYz7Uk+9ucax4oH1vNSj/zcURB7D2lrMBnavPEZypCrmIs6utGevRg0LAhDJtqLtnTJ1u4EY2lEplRPUTFtet4auEACAHpzT5rwYIG2Ja3r3uF/GDLe1HwQ+qUFxDuVcxYyJcZHHnEdbo9hqPAAAAFQCGx4RDye/N2AtQQowZukGg0qb3YwAAAIEAuXZK93DUGUVT3V9tYFOzG+NJHnWJWaxj5a41ZMdCYxR5f8A2+G60wfVvn81FTcDs7LbZQ4HH6ThiV+0CJmoT/fulCxNmkre9C65Uer4CSx6sCw9Vnb/kKjGiiNsWFUF6xTAjSdY8JGi9W8IQRI29a/TlZoxITSb6wmWVy+iyCAwAAACAeJa0vZUyDImSVgMaERtDsSUd5UzMSAJCJxYwgZvBRdFt/Zo606Sh0XisUjF14IHqRI3IAhpncssplZT96KxRgHG57+mFWfUGkKFCUoAJGAoLHkjYyWUs+VRO9IsSicV9v229rlv6LNrvnSPMM6d+vvWTqezgs0pFe4n5HdD+Zmk="
                },
                {
                  "idShort":"ssh_host_key_dsa_public_keytype",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"ssh-dss"
                },
                {
                  "idShort":"ssh_host_key_rsa_public",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"AAAAB3NzaC1yc2EAAAADAQABAAABgQDcUkD5RGvCp1Esy6OEmW5GdlO33Pfx/CM3CAEpUdM7RWMEyCPNipnPdlT7+ur68resNDHQDSk2abSaOf/rpMb/PgwZISFu3Q/gUv8NPNNIt/ZxkHizH3XOpEy+pXICPYlDw+lz+go8yW+r05h6BY6cqfB9/cPn2k0KDq7pTQDtYsnUU+4Ii4/LVRt1jAoz8Rh1TN/Y6Yv2jHX56U1142+v4ckc7a/wxiuhlu5wAYHrAZ3Tro3G86+NYi2W7oODt3oYaZF8MeRxdLHIg9cjQZYephZtY1veXWMnC1UjVj6fkMWrIMxbDOMviWjqacdr6atAwnXKG7Yuy7BW8tF19tD5IybXUSNTuSqVBQTRFH7GQsXdQMxFdeHG4SdHvzhR10YrXowNyP+4CQ5PINkYn+RGCkovUXss+Qvl4zkbEZFdisuOQTFWiBoou981p+oMkh2VAUOrmVdC6HCy4NGSHNi6YjR5fpRvb7Lv4jSoiLvRSUm5jmyGQvCVpCRfHYy8Z4E="
                },
                {
                  "idShort":"ssh_host_key_rsa_public_keytype",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"ssh-rsa"
                },
                {
                  "idShort":"ssh_host_key_ecdsa_public",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTYAAABBBDKaJssYSy/wY9teifmsDbWfFN3ryR30rSZw9sh0pnOvq6LRekVlQRhNj43bUKvdHOf9wpIO2PmzBZ0umt9QKiE="
                },
                {
                  "idShort":"ssh_host_key_ecdsa_public_keytype",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"ecdsa-sha2-nistp256"
                },
                {
                  "idShort":"ssh_host_key_ed25519_public",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"AAAAC3NzaC1lZDI1NTE5AAAAINUxhEixcxmARg7adYce+bZlV7YWZa7VCNw+yOWCdDDF"
                },
                {
                  "idShort":"ssh_host_key_ed25519_public_keytype",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"ssh-ed25519"
                },
                {
                  "idShort":"iscsi_iqn",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":""
                },
                {
                  "idShort":"hostnqn",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":""
                },
                {
                  "idShort":"bios_date",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"08/07/2020"
                },
                {
                  "idShort":"bios_vendor",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"VMware, Inc."
                },
                {
                  "idShort":"bios_version",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"VMW71.00V.16707776.B64.2008070230"
                },
                {
                  "idShort":"board_asset_tag",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"NA"
                },
                {
                  "idShort":"board_name",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"440BX Desktop Reference Platform"
                },
                {
                  "idShort":"board_serial",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"NA"
                },
                {
                  "idShort":"board_vendor",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"Intel Corporation"
                },
                {
                  "idShort":"board_version",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"None"
                },
                {
                  "idShort":"chassis_asset_tag",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"No Asset Tag"
                },
                {
                  "idShort":"chassis_serial",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"NA"
                },
                {
                  "idShort":"chassis_vendor",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"No Enclosure"
                },
                {
                  "idShort":"chassis_version",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"N/A"
                },
                {
                  "idShort":"form_factor",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"Other"
                },
                {
                  "idShort":"product_name",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"VMware7,1"
                },
                {
                  "idShort":"product_serial",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"NA"
                },
                {
                  "idShort":"product_uuid",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"NA"
                },
                {
                  "idShort":"product_version",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"None"
                },
                {
                  "idShort":"system_vendor",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"VMware, Inc."
                },
                {
                  "idShort":"lvm",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"N/A"
                },
                {
                  "idShort":"pkg_mgr",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"apt"
                },
                {
                  "idShort":"service_mgr",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"systemd"
                },
                {
                  "idShort":"discovered_interpreter_python",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"string",
                  "value":"/usr/bin/python3"
                },
                {
                  "idShort":"virtualization_tech_guest",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"anyType",
                  "value":"['VMware']"
                },
                {
                  "idShort":"virtualization_tech_host",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"anyType",
                  "value":"['kvm']"
                },
                {
                  "idShort":"system_capabilities",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"anyType",
                  "value":"['']"
                },
                {
                  "idShort":"fibre_channel_wwn",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"anyType",
                  "value":"[]"
                },
                {
                  "idShort":"interfaces",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"anyType",
                  "value":"['ens192', 'lo']"
                },
                {
                  "idShort":"all_ipv4_addresses",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"anyType",
                  "value":"['10.1.9.25']"
                },
                {
                  "idShort":"all_ipv6_addresses",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"anyType",
                  "value":"['fe80::250:56ff:febb:cca9']"
                },
                {
                  "idShort":"processor",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"anyType",
                  "value":"['0', 'GenuineIntel', 'Intel(R) Xeon(R) Silver 4116 CPU @ 2.10GHz', '1', 'GenuineIntel', 'Intel(R) Xeon(R) Silver 4116 CPU @ 2.10GHz']"
                },
                {
                  "idShort":"mounts",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"anyType",
                  "value":"[{'mount': '/', 'device': '/dev/mapper/ubuntu--vg-ubuntu--lv', 'fstype': 'ext4', 'options': 'rw,relatime', 'size_total': 19342737408, 'size_available': 12206751744, 'block_size': 4096, 'block_total': 4722348, 'block_available': 2980164, 'block_used': 1742184, 'inode_total': 1212416, 'inode_available': 1121776, 'inode_used': 90640, 'uuid': 'be84b207-8343-4f98-9e4e-552a3e3869a7'}, {'mount': '/snap/core20/1587', 'device': '/dev/loop2', 'fstype': 'squashfs', 'options': 'ro,nodev,relatime,errors=continue', 'size_total': 65011712, 'size_available': 0, 'block_size': 131072, 'block_total': 496, 'block_available': 0, 'block_used': 496, 'inode_total': 11793, 'inode_available': 0, 'inode_used': 11793, 'uuid': 'N/A'}, {'mount': '/snap/lxd/22923', 'device': '/dev/loop0', 'fstype': 'squashfs', 'options': 'ro,nodev,relatime,errors=continue', 'size_total': 83886080, 'size_available': 0, 'block_size': 131072, 'block_total': 640, 'block_available': 0, 'block_used': 640, 'inode_total': 816, 'inode_available': 0, 'inode_used': 816, 'uuid': 'N/A'}, {'mount': '/snap/snapd/16292', 'device': '/dev/loop1', 'fstype': 'squashfs', 'options': 'ro,nodev,relatime,errors=continue', 'size_total': 49283072, 'size_available': 0, 'block_size': 131072, 'block_total': 376, 'block_available': 0, 'block_used': 376, 'inode_total': 486, 'inode_available': 0, 'inode_used': 486, 'uuid': 'N/A'}, {'mount': '/boot', 'device': '/dev/sda2', 'fstype': 'ext4', 'options': 'rw,relatime', 'size_total': 2040373248, 'size_available': 1782423552, 'block_size': 4096, 'block_total': 498138, 'block_available': 435162, 'block_used': 62976, 'inode_total': 131072, 'inode_available': 130766, 'inode_used': 306, 'uuid': 'f8adc484-c634-4e02-a1e8-5ffd2ad48511'}, {'mount': '/boot/efi', 'device': '/dev/sda1', 'fstype': 'vfat', 'options': 'rw,relatime,fmask=0022,dmask=0022,codepage=437,iocharset=iso8859-1,shortname=mixed,errors=remount-ro', 'size_total': 1124999168, 'size_available': 1118633984, 'block_size': 4096, 'block_total': 274658, 'block_available': 273104, 'block_used': 1554, 'inode_total': 0, 'inode_available': 0, 'inode_used': 0, 'uuid': '2478-5C5A'}]"
                },
                {
                  "idShort":"gather_subset",
                  "modelType":{
                    "name":"Property"
                  },
                  "valueType":"anyType",
                  "value":"['all']"
                }
              ]
            }
            """;

}
