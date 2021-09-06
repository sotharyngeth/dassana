/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
import { Error404 } from '@dassana-io/web-components'
import Layout from '@theme/Layout'
import styles from './NotFound.module.css'
import { useHistory } from 'react-router'
import React, { FC } from 'react'

const NotFound: FC = () => {
	const history = useHistory()

	return (
		<Layout title='Page Not Found'>
			<div className={styles.wrapper}>
				<Error404 onBtnClick={() => history.push('/')} />
			</div>
		</Layout>
	)
}

export default NotFound
