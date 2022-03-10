#!/usr/bin/env python3

import sys
import csv
import re
import click

from cdbApi import SimpleLocationInformation
from cdbApi import LogEntryEditInformation
from cdbApi import ItemStatusBasicObject
from cdbApi import ApiException

from CdbApiFactory import CdbApiFactory

from cdbCli.common.cli.cliBase import CliBase
from cdbCli.service.cli.CDBclickCmnds.setItemLogById import set_item_log_by_id_help


##############################################################################################
#                                                                                            #
#                             Set the QR ID of an inventory item                             #
#                                                                                            #
##############################################################################################
def set_qr_id_by_id_help(item_id, qrid, cli):
    """
    This function sets a new QR ID for a given item

        :param item_id: The ID of the item
        :param qrid: The new desired QR ID of the item
        :param cli: necessary CliBase object
    """

    factory = cli.require_authenticated_api()
    item_api = factory.getItemApi()

    try:
        item = item_api.get_item_by_id(item_id)
        old_qr_id = str(item.qr_id)
        item.qr_id = int(qrid)
        item_api.update_item_details(item=item)
        echo_string = "Item ID:" + str(item_id) + ",Old QRId:" + str(old_qr_id) + ",New QRId:" +str(qrid)
        click.echo(echo_string)
    except ApiException as e:
        p = r'"localizedMessage.*'
        matches = re.findall(p, e.body)
        if matches:
            error = "Error setting QR ID: " + matches[0][:-2]
            click.echo(error)
        else:
            click.echo("Error setting QR ID")


@click.command()
@click.option('--inputfile',help='Input csv file with new location parameters, see help, default is STDIN',
              type=click.File('r'),default=sys.stdin)
@click.option('--dist', help='Change the CDB distribution (as provided in cdb.conf)')
def set_qr_id_by_id(inputfile,  dist=None):
    """Assigns QR Ids to a set of Item IDs.  This will overwrite QR Codes if already assigned.
       CSV input is on STDIN(default) or a file and the csv format is
       <Item ID>,<QR Code>"""
    

    cli = CliBase(dist)
    factory = cli.require_authenticated_api()
    item_api = factory.getItemApi()
    
    reader = csv.reader(inputfile)
    for row in reader:
        item_id = row[0]
        qr_id = row[1]
        set_qr_id_by_id_help(item_id, qr_id, cli)


if __name__ == "__main__":
    set_qr_id_by_id()

